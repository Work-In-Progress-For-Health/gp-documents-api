import logging
import uuid
from typing import Optional

from fhir.resources.binary import Binary
from fhir.resources.bundle import Bundle
from fhir.resources.documentreference import DocumentReference
from fhir.resources.patient import Patient
from pydantic import BaseModel

from ..config import ProcessingMode, Settings
from ..models import QuarantineMessage
from .clamav_service import ClamAVService
from .minio_service import MinIOService, StorageResult
from .rabbitmq_service import RabbitMQService

logger = logging.getLogger(__name__)


class ProcessingResult(BaseModel):
    """Result of document processing."""
    success: bool
    scan_status: Optional[str] = None
    error_message: Optional[str] = None
    malware_detected: bool = False
    storage_result: Optional[StorageResult] = None


class DocumentProcessingService:
    """Service for processing documents based on configured mode."""

    def __init__(
        self,
        settings: Settings,
        clamav_service: ClamAVService,
        minio_service: MinIOService,
        rabbitmq_service: RabbitMQService
    ):
        self.processing_mode = settings.processing_mode
        self.clamav_service = clamav_service
        self.minio_service = minio_service
        self.rabbitmq_service = rabbitmq_service

        logger.info(f"Document processing mode: {self.processing_mode}")

    def process_document(
        self,
        gp_practice_id: str,
        bundle: Bundle,
        bundle_json: str,
        binary: Binary,
        base64_data: str
    ) -> ProcessingResult:
        """
        Process document based on configured mode.

        Args:
            gp_practice_id: GP Practice ID
            bundle: FHIR Bundle
            bundle_json: Bundle as JSON string
            binary: Binary resource
            base64_data: Base64-encoded document data

        Returns:
            ProcessingResult
        """
        if self.processing_mode == ProcessingMode.SYNC:
            return self._process_synchronous(base64_data)
        elif self.processing_mode == ProcessingMode.ASYNC:
            return self._process_asynchronous(gp_practice_id, bundle, bundle_json, binary, base64_data)
        elif self.processing_mode == ProcessingMode.HYBRID:
            return self._process_hybrid(gp_practice_id, bundle, bundle_json, binary, base64_data)
        else:
            raise ValueError(f"Unknown processing mode: {self.processing_mode}")

    def _process_synchronous(self, base64_data: str) -> ProcessingResult:
        """SYNC Mode: Check binary with ClamAV, respond based on result."""
        logger.info("Processing in SYNC mode")

        scan_result = self.clamav_service.scan_document(base64_data)

        if not scan_result.is_clean():
            return ProcessingResult(
                success=False,
                error_message=f"Malware detected in submitted document: {scan_result.get_details()}",
                malware_detected=True
            )

        return ProcessingResult(
            success=True,
            scan_status="CLEAN"
        )

    def _process_asynchronous(
        self,
        gp_practice_id: str,
        bundle: Bundle,
        bundle_json: str,
        binary: Binary,
        base64_data: str
    ) -> ProcessingResult:
        """
        ASYNC Mode: Validate syntax/format, respond immediately,
        store binary in MinIO and send to RabbitMQ for offline processing.
        """
        logger.info("Processing in ASYNC mode")

        # Extract patient information
        nhs_number = self._extract_nhs_number(bundle)
        document_reference_id = self._extract_document_reference_id(bundle)

        # Store in MinIO
        storage_result = self.minio_service.store_document(
            base64_data,
            nhs_number,
            binary.contentType
        )

        if not storage_result.success:
            return ProcessingResult(
                success=False,
                error_message=f"Failed to store document: {storage_result.error_message}"
            )

        # Send to RabbitMQ
        try:
            message = QuarantineMessage(
                object_name=storage_result.object_name,
                bucket_name=storage_result.bucket_name,
                etag=None,
                size=storage_result.size,
                content_type=storage_result.content_type,
                submission_id=str(uuid.uuid4()),
                patient_id=nhs_number,
                document_reference_id=document_reference_id,
                original_filename=storage_result.object_name
            )

            self.rabbitmq_service.send_quarantine_message(message)

            return ProcessingResult(
                success=True,
                scan_status="PENDING_ASYNC",
                storage_result=storage_result
            )

        except Exception as e:
            logger.error(f"Failed to send message to RabbitMQ: {e}")
            return ProcessingResult(
                success=False,
                error_message=f"Failed to queue document for processing: {str(e)}"
            )

    def _process_hybrid(
        self,
        gp_practice_id: str,
        bundle: Bundle,
        bundle_json: str,
        binary: Binary,
        base64_data: str
    ) -> ProcessingResult:
        """
        HYBRID Mode: Check binary with ClamAV, respond based on result,
        if clean store in MinIO and send to RabbitMQ for deeper inspection.
        """
        logger.info("Processing in HYBRID mode")

        # First, scan with ClamAV
        scan_result = self.clamav_service.scan_document(base64_data)

        if not scan_result.is_clean():
            return ProcessingResult(
                success=False,
                error_message=f"Malware detected in submitted document: {scan_result.get_details()}",
                malware_detected=True
            )

        # Document is clean, now store and queue for deeper inspection
        nhs_number = self._extract_nhs_number(bundle)
        document_reference_id = self._extract_document_reference_id(bundle)

        # Store in MinIO
        storage_result = self.minio_service.store_document(
            base64_data,
            nhs_number,
            binary.contentType
        )

        if not storage_result.success:
            return ProcessingResult(
                success=False,
                error_message=f"Failed to store document: {storage_result.error_message}"
            )

        # Send to RabbitMQ for deeper inspection
        try:
            message = QuarantineMessage(
                object_name=storage_result.object_name,
                bucket_name=storage_result.bucket_name,
                etag=None,
                size=storage_result.size,
                content_type=storage_result.content_type,
                submission_id=str(uuid.uuid4()),
                patient_id=nhs_number,
                document_reference_id=document_reference_id,
                original_filename=storage_result.object_name
            )

            self.rabbitmq_service.send_quarantine_message(message)

            return ProcessingResult(
                success=True,
                scan_status="CLEAN",
                storage_result=storage_result
            )

        except Exception as e:
            logger.error(f"Failed to send message to RabbitMQ: {e}")
            # Document is still clean and accepted, but couldn't queue for deep scan
            logger.warning("Document accepted but not queued for deep inspection")
            return ProcessingResult(
                success=True,
                scan_status="CLEAN",
                storage_result=storage_result
            )

    @staticmethod
    def _extract_nhs_number(bundle: Bundle) -> str:
        """Extract NHS number from bundle."""
        for entry in bundle.entry:
            if isinstance(entry.resource, Patient):
                patient = entry.resource
                for identifier in patient.identifier or []:
                    if identifier.system == "https://fhir.nhs.uk/Id/nhs-number" and identifier.value:
                        return identifier.value
        return "UNKNOWN"

    @staticmethod
    def _extract_document_reference_id(bundle: Bundle) -> str:
        """Extract DocumentReference ID from bundle."""
        for entry in bundle.entry:
            if isinstance(entry.resource, DocumentReference):
                doc_ref = entry.resource
                if doc_ref.id:
                    return doc_ref.id
        return str(uuid.uuid4())
