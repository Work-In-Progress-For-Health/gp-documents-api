import base64
import logging
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Request, status
from fhir.resources.binary import Binary
from fhir.resources.bundle import Bundle
from fhir.resources.patient import Patient
from sqlalchemy.orm import Session

from ..config import get_settings
from ..database import get_db
from ..models import FhirOperationOutcome
from ..repositories import GpPracticeRepository
from ..services import (
    ClamAVService,
    DocumentProcessingService,
    MinIOService,
    RabbitMQService,
)

logger = logging.getLogger(__name__)

router = APIRouter()

REQUIRED_RESOURCES = ["DocumentReference", "Binary", "Patient", "Encounter", "Practitioner", "Organization"]


@router.post(
    "/api/v1/gp-practice/{gp_practice_id}/documents",
    response_model=FhirOperationOutcome,
    status_code=status.HTTP_201_CREATED
)
async def submit_document_bundle(
    gp_practice_id: str,
    request: Request,
    db: Session = Depends(get_db)
) -> FhirOperationOutcome:
    """
    Submit a FHIR Bundle containing clinical documents.

    Args:
        gp_practice_id: GP Practice ID
        request: HTTP request
        db: Database session

    Returns:
        FhirOperationOutcome

    Raises:
        HTTPException: For various error conditions
    """
    settings = get_settings()

    # Get request body as text
    bundle_json = await request.body()
    bundle_json = bundle_json.decode("utf-8")

    # Check if GP practice exists
    gp_practice_repo = GpPracticeRepository(db)
    if not gp_practice_repo.exists_by_gp_practice_id(gp_practice_id):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=FhirOperationOutcome.error(f"Invalid GP practice ID: {gp_practice_id}").model_dump()
        )

    # Parse the incoming JSON to a FHIR Bundle
    try:
        bundle = Bundle.parse_raw(bundle_json)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=FhirOperationOutcome.error(f"Invalid FHIR Bundle JSON: {str(e)}").model_dump()
        )

    if not bundle or not bundle.type or not bundle.entry:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=FhirOperationOutcome.error("Invalid or missing FHIR Bundle structure.").model_dump()
        )

    # Extract Binary resource
    binary_resource: Optional[Binary] = None
    for entry in bundle.entry:
        if isinstance(entry.resource, Binary):
            binary_resource = entry.resource
            break

    if not binary_resource:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=FhirOperationOutcome.error("Missing Binary resource in bundle.").model_dump()
        )

    if not binary_resource.data:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=FhirOperationOutcome.error("Binary resource contains no data.").model_dump()
        )

    # Convert binary data to base64 string (it's already base64 in FHIR)
    base64_data = binary_resource.data

    # Validate NHS Number is present and verified
    nhs_validation_error = _validate_nhs_number(bundle)
    if nhs_validation_error:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=FhirOperationOutcome.error(nhs_validation_error).model_dump()
        )

    # Validate the bundle using fhir.resources validation
    # Note: fhir.resources performs validation during parsing, so if we got here, basic structure is valid

    # Check for required resources
    resource_types = [entry.resource.resource_type for entry in bundle.entry if entry.resource]

    for required in REQUIRED_RESOURCES:
        if required not in resource_types:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=FhirOperationOutcome.error(f"Missing mandatory FHIR resource: {required}").model_dump()
            )

    # Process document based on configured mode
    clamav_service = ClamAVService(settings)
    minio_service = MinIOService(settings)
    rabbitmq_service = RabbitMQService(settings)
    processing_service = DocumentProcessingService(
        settings, clamav_service, minio_service, rabbitmq_service
    )

    result = processing_service.process_document(
        gp_practice_id, bundle, bundle_json, binary_resource, base64_data
    )

    if not result.success:
        if result.malware_detected:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail=FhirOperationOutcome.error(result.error_message).model_dump()
            )
        else:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=FhirOperationOutcome.error(result.error_message).model_dump()
            )

    return FhirOperationOutcome.success(
        f"Bundle successfully processed and document accepted for GP practice {gp_practice_id}"
    )


def _validate_nhs_number(bundle: Bundle) -> Optional[str]:
    """
    Validate that the Patient resource has a verified NHS Number.

    Args:
        bundle: FHIR Bundle

    Returns:
        Error message if validation fails, None if valid
    """
    # Find Patient resource
    patient_resource: Optional[Patient] = None
    for entry in bundle.entry:
        if isinstance(entry.resource, Patient):
            patient_resource = entry.resource
            break

    if not patient_resource:
        return "Patient resource not found in bundle"

    # Check for NHS Number identifier
    nhs_number_identifier = None
    for identifier in patient_resource.identifier or []:
        if identifier.system == "https://fhir.nhs.uk/Id/nhs-number":
            nhs_number_identifier = identifier
            break

    if not nhs_number_identifier:
        return "NHS Number identifier not found in Patient resource"

    # Check for NHS Number value
    if not nhs_number_identifier.value:
        return "NHS Number value is missing"

    # Check for verification status extension
    verification_extension = None
    for ext in nhs_number_identifier.extension or []:
        if ext.url == "https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-NHSNumberVerificationStatus":
            verification_extension = ext
            break

    if not verification_extension:
        return "NHS Number verification status extension not found"

    # Check that extension has a CodeableConcept value
    if not verification_extension.valueCodeableConcept:
        return "NHS Number verification status extension does not contain a CodeableConcept"

    codeable_concept = verification_extension.valueCodeableConcept

    # Check for coding
    if not codeable_concept.coding:
        return "NHS Number verification status does not contain coding"

    # Check that at least one coding has the correct system and code
    has_valid_verification = False
    for coding in codeable_concept.coding:
        if (coding.system == "https://fhir.hl7.org.uk/CodeSystem/UKCore-NHSNumberVerificationStatus" and
                coding.code in ["01", "number-present-and-verified"]):
            has_valid_verification = True
            break

    if not has_valid_verification:
        return "NHS Number is not verified. Verification status must be '01' or 'number-present-and-verified'"

    return None  # Validation passed
