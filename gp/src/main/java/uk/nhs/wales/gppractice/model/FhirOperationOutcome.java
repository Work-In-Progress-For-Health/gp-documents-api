package uk.nhs.wales.gppractice.model;

import org.hl7.fhir.r4.model.OperationOutcome;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FhirOperationOutcome {
    private final String resourceType = "OperationOutcome";
    private List<Map<String, Object>> issue;

    public String getResourceType() { 
        return resourceType; 
    }
    
    public List<Map<String, Object>> getIssue() { 
        return issue; 
    }
    
    public void setIssue(List<Map<String, Object>> issue) { 
        this.issue = issue; 
    }

    public static FhirOperationOutcome success(String message) {
        return create("information", "informational", message);
    }

    public static FhirOperationOutcome error(String message) {
        return create("error", "invalid", message);
    }

    public static FhirOperationOutcome fromOperationOutcome(OperationOutcome operationOutcome) {
        FhirOperationOutcome outcome = new FhirOperationOutcome();
        
        if (operationOutcome.hasIssue()) {
            outcome.setIssue(
                operationOutcome.getIssue().stream()
                    .map(issue -> Map.of(
                        "severity", issue.hasSeverity() ? issue.getSeverity().toCode() : "error",
                        "code", issue.hasCode() ? issue.getCode().toCode() : "unknown",
                        "details", Map.of(
                            "text", issue.hasDiagnostics() ? issue.getDiagnostics() : 
                                   (issue.hasDetails() && issue.getDetails().hasText() ? 
                                    issue.getDetails().getText() : "Validation failed")
                        )
                    ))
                    .collect(Collectors.toList())
            );
        } else {
            outcome.setIssue(List.of(Map.of(
                "severity", "error",
                "code", "unknown",
                "details", Map.of("text", "Validation failed")
            )));
        }
        
        return outcome;
    }

    private static FhirOperationOutcome create(String severity, String code, String message) {
        FhirOperationOutcome outcome = new FhirOperationOutcome();
        outcome.setIssue(List.of(Map.of(
                "severity", severity,
                "code", code,
                "details", Map.of("text", message)
        )));
        return outcome;
    }
}
