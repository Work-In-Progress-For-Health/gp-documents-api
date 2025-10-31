package uk.nhs.wales.gppractice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.nhs.wales.gppractice.repository.GpPracticeRepository;
import uk.nhs.wales.gppractice.model.FhirOperationOutcome;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gp-practice/{gpPracticeId}/documents")
public class DocumentSubmissionController {

    private final GpPracticeRepository gpPracticeRepository;
    private final FhirValidator fhirValidator;
    private final FhirContext fhirContext;

    private static final List<String> REQUIRED_RESOURCES = List.of(
            "DocumentReference", "Binary", "Patient", "Encounter", "Practitioner", "Organization"
    );

    @Autowired
    public DocumentSubmissionController(GpPracticeRepository gpPracticeRepository, FhirValidator fhirValidator) {
        this.gpPracticeRepository = gpPracticeRepository;
        this.fhirValidator = fhirValidator;
        this.fhirContext = FhirContext.forR4();
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
}
