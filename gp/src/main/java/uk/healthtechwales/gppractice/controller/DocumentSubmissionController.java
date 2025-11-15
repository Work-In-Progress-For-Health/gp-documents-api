package uk.healthtechwales.gppractice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.healthtechwales.gppractice.repository.GpPracticeRepository;
import uk.healthtechwales.gppractice.model.FhirOperationOutcome;
import uk.healthtechwales.gppractice.service.DocumentProcessingService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.OperationOutcome;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/gp-practice/{gpPracticeId}/documents")
public class DocumentSubmissionController {

    private final GpPracticeRepository gpPracticeRepository;
    private final FhirValidator fhirValidator;
    private final FhirContext fhirContext;
    private final DocumentProcessingService documentProcessingService;

    private static final List<String> REQUIRED_RESOURCES = List.of(
            "DocumentReference", "Binary", "Patient", "Encounter", "Practitioner", "Organization"
    );

    @Autowired
    public DocumentSubmissionController(
            GpPracticeRepository gpPracticeRepository, 
            FhirValidator fhirValidator,
            DocumentProcessingService documentProcessingService) {
        this.gpPracticeRepository = gpPracticeRepository;
        this.fhirValidator = fhirValidator;
        this.fhirContext = FhirContext.forR4();
        this.documentProcessingService = documentProcessingService;
    }

    @PostMapping(consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<FhirOperationOutcome> submitDocumentBundle(
            @PathVariable("gpPracticeId") String gpPracticeId,
            @RequestBody String bundleJson) {

        boolean gpExists = gpPracticeRepository.existsByGpPracticeId(gpPracticeId);
        if (!gpExists) {
            return invalidPractice("Invalid GP practice ID: " + gpPracticeId);
        }

        // Parse the incoming JSON to a FHIR Bundle
        Bundle bundle;
        try {
            bundle = fhirContext.newJsonParser().parseResource(Bundle.class, bundleJson);
        } catch (Exception e) {
            return badRequest("Invalid FHIR Bundle JSON: " + e.getMessage());
        }

        if (bundle == null || bundle.getType() == null || bundle.getEntry() == null || bundle.getEntry().isEmpty()) {
            return badRequest("Invalid or missing FHIR Bundle structure.");
        }

        // Extract Binary resource
        Optional<Binary> binaryResource = bundle.getEntry().stream()
                .filter(entry -> entry.getResource() != null)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof Binary)
                .map(resource -> (Binary) resource)
                .findFirst();

        if (binaryResource.isEmpty()) {
            return badRequest("Missing Binary resource in bundle.");
        }

        Binary binary = binaryResource.get();
        if (binary.getData() == null || binary.getData().length == 0) {
            return badRequest("Binary resource contains no data.");
        }

        // Convert binary data to base64 string
        String base64Data = java.util.Base64.getEncoder().encodeToString(binary.getData());

        // Validate NHS Number is present and verified
        String nhsNumberValidation = validateNHSNumber(bundle);
        if (nhsNumberValidation != null) {
            return badRequest(nhsNumberValidation);
        }

        // Validate the bundle
        ValidationResult validationResult = fhirValidator.validateWithResult(bundle);
        if (!validationResult.isSuccessful()) {
            OperationOutcome outcome = (OperationOutcome) validationResult.toOperationOutcome();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(FhirOperationOutcome.fromOperationOutcome(outcome));
        }

        // Check for required resources
        List<String> resourceTypes = bundle.getEntry().stream()
                .filter(entry -> entry.getResource() != null)
                .map(entry -> entry.getResource().fhirType())
                .toList();

        for (String required : REQUIRED_RESOURCES) {
            if (!resourceTypes.contains(required)) {
                return badRequest("Missing mandatory FHIR resource: " + required);
            }
        }

        // Process document based on configured mode
        DocumentProcessingService.ProcessingResult result = documentProcessingService.processDocument(
                gpPracticeId, bundle, bundleJson, binary, base64Data);

        if (!result.isSuccess()) {
            if (result.isMalwareDetected()) {
                return malwareDetected(result.getErrorMessage());
            } else {
                return badRequest(result.getErrorMessage());
            }
        }

        FhirOperationOutcome outcome = FhirOperationOutcome.success(
                "Bundle successfully processed and document accepted for GP practice " + gpPracticeId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(outcome);
    }

    private ResponseEntity<FhirOperationOutcome> badRequest(String message) {
        FhirOperationOutcome outcome = FhirOperationOutcome.error(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(outcome);
    }

    private ResponseEntity<FhirOperationOutcome> invalidPractice(String message) {
        FhirOperationOutcome outcome = FhirOperationOutcome.error(message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(outcome);
    }

    private ResponseEntity<FhirOperationOutcome> malwareDetected(String message) {
        FhirOperationOutcome outcome = FhirOperationOutcome.error(message);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(outcome);
    }

    /**
     * Validates that the Patient resource has a verified NHS Number
     * @param bundle the FHIR Bundle
     * @return error message if validation fails, null if valid
     */
    private String validateNHSNumber(Bundle bundle) {
        // Find Patient resource
        Optional<org.hl7.fhir.r4.model.Patient> patientResource = bundle.getEntry().stream()
                .filter(entry -> entry.getResource() != null)
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource instanceof org.hl7.fhir.r4.model.Patient)
                .map(resource -> (org.hl7.fhir.r4.model.Patient) resource)
                .findFirst();

        if (patientResource.isEmpty()) {
            return "Patient resource not found in bundle";
        }

        org.hl7.fhir.r4.model.Patient patient = patientResource.get();

        // Check for NHS Number identifier
        Optional<org.hl7.fhir.r4.model.Identifier> nhsNumberIdentifier = patient.getIdentifier().stream()
                .filter(identifier -> identifier.hasSystem() && 
                        identifier.getSystem().equals("https://fhir.nhs.uk/Id/nhs-number"))
                .findFirst();

        if (nhsNumberIdentifier.isEmpty()) {
            return "NHS Number identifier not found in Patient resource";
        }

        org.hl7.fhir.r4.model.Identifier identifier = nhsNumberIdentifier.get();

        // Check for NHS Number value
        if (!identifier.hasValue() || identifier.getValue().isEmpty()) {
            return "NHS Number value is missing";
        }

        // Check for verification status extension
        Optional<org.hl7.fhir.r4.model.Extension> verificationExtension = identifier.getExtension().stream()
                .filter(ext -> ext.hasUrl() && 
                        ext.getUrl().equals("https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-NHSNumberVerificationStatus"))
                .findFirst();

        if (verificationExtension.isEmpty()) {
            return "NHS Number verification status extension not found";
        }

        org.hl7.fhir.r4.model.Extension extension = verificationExtension.get();

        // Check that extension has a CodeableConcept value
        if (!extension.hasValue() || !(extension.getValue() instanceof org.hl7.fhir.r4.model.CodeableConcept)) {
            return "NHS Number verification status extension does not contain a CodeableConcept";
        }

        org.hl7.fhir.r4.model.CodeableConcept codeableConcept = (org.hl7.fhir.r4.model.CodeableConcept) extension.getValue();

        // Check for coding
        if (!codeableConcept.hasCoding() || codeableConcept.getCoding().isEmpty()) {
            return "NHS Number verification status does not contain coding";
        }

        // Check that at least one coding has the correct system and code
        boolean hasValidVerification = codeableConcept.getCoding().stream()
                .anyMatch(coding -> 
                    coding.hasSystem() && 
                    coding.getSystem().equals("https://fhir.hl7.org.uk/CodeSystem/UKCore-NHSNumberVerificationStatus") &&
                    coding.hasCode() && 
                    (coding.getCode().equals("01") || coding.getCode().equals("number-present-and-verified"))
                );

        if (!hasValidVerification) {
            return "NHS Number is not verified. Verification status must be '01' or 'number-present-and-verified'";
        }

        return null; // Validation passed
    }
}
