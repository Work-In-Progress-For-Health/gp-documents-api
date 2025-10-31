package uk.nhs.wales.gppractice.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.stereotype.Service;
import uk.nhs.wales.gppractice.model.FhirOperationOutcome;

@Service
public class FhirValidationService {

    private final FhirValidator validator;
    private final FhirContext fhirContext;

    public FhirValidationService(FhirValidator validator, FhirContext fhirContext) {
        this.validator = validator;
        this.fhirContext = fhirContext;
    }

    public FhirOperationOutcome validateBundle(String bundleJson) {
        Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(bundleJson);
        ValidationResult result = validator.validateWithResult(bundle);

        if (result.isSuccessful()) {
            return FhirOperationOutcome.success("FHIR Bundle validation passed.");
        } else {
            OperationOutcome outcome = (OperationOutcome) result.toOperationOutcome();
            String details = outcome.getIssue().stream()
                    .map(i -> i.getSeverity().toCode() + ": " + i.getDiagnostics())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown validation error");
            return FhirOperationOutcome.error("FHIR validation failed: " + details);
        }
    }
}

