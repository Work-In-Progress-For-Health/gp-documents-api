from typing import Any

from pydantic import BaseModel, Field


class FhirOperationOutcome(BaseModel):
    """FHIR OperationOutcome resource for API responses."""

    resourceType: str = Field(default="OperationOutcome", description="FHIR resource type")
    issue: list[dict[str, Any]] = Field(..., description="List of issues")

    @classmethod
    def success(cls, message: str) -> "FhirOperationOutcome":
        """Create a success OperationOutcome."""
        return cls(
            issue=[
                {
                    "severity": "information",
                    "code": "informational",
                    "details": {"text": message}
                }
            ]
        )

    @classmethod
    def error(cls, message: str) -> "FhirOperationOutcome":
        """Create an error OperationOutcome."""
        return cls(
            issue=[
                {
                    "severity": "error",
                    "code": "invalid",
                    "details": {"text": message}
                }
            ]
        )

    @classmethod
    def from_validation_result(cls, validation_result: Any) -> "FhirOperationOutcome":
        """Create OperationOutcome from FHIR validation result."""
        # This will be implemented based on the fhir.resources validation result structure
        issues = []

        if hasattr(validation_result, "issues"):
            for issue in validation_result.issues:
                issues.append({
                    "severity": getattr(issue, "severity", "error"),
                    "code": getattr(issue, "code", "unknown"),
                    "details": {
                        "text": getattr(issue, "diagnostics", getattr(issue, "details", "Validation failed"))
                    }
                })

        if not issues:
            issues = [
                {
                    "severity": "error",
                    "code": "unknown",
                    "details": {"text": "Validation failed"}
                }
            ]

        return cls(issue=issues)

    class Config:
        populate_by_name = True
