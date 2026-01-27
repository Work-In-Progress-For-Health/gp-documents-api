using System.Text.Json.Serialization;
using Hl7.Fhir.Model;

namespace Uk.HealthTechWales.GpPractice.Models;

public class FhirOperationOutcome
{
    [JsonPropertyName("resourceType")]
    public string ResourceType { get; } = "OperationOutcome";

    [JsonPropertyName("issue")]
    public List<Dictionary<string, object>>? Issue { get; set; }

    public static FhirOperationOutcome Success(string message)
    {
        return Create("information", "informational", message);
    }

    public static FhirOperationOutcome Error(string message)
    {
        return Create("error", "invalid", message);
    }

    public static FhirOperationOutcome FromOperationOutcome(OperationOutcome operationOutcome)
    {
        var outcome = new FhirOperationOutcome();

        if (operationOutcome.Issue != null && operationOutcome.Issue.Any())
        {
            outcome.Issue = operationOutcome.Issue
                .Select(issue => new Dictionary<string, object>
                {
                    ["severity"] = issue.Severity?.ToString()?.ToLower() ?? "error",
                    ["code"] = issue.Code?.ToString()?.ToLower() ?? "unknown",
                    ["details"] = new Dictionary<string, object>
                    {
                        ["text"] = issue.Diagnostics ??
                                  (issue.Details?.Text ?? "Validation failed")
                    }
                })
                .ToList();
        }
        else
        {
            outcome.Issue = new List<Dictionary<string, object>>
            {
                new()
                {
                    ["severity"] = "error",
                    ["code"] = "unknown",
                    ["details"] = new Dictionary<string, object> { ["text"] = "Validation failed" }
                }
            };
        }

        return outcome;
    }

    private static FhirOperationOutcome Create(string severity, string code, string message)
    {
        return new FhirOperationOutcome
        {
            Issue = new List<Dictionary<string, object>>
            {
                new()
                {
                    ["severity"] = severity,
                    ["code"] = code,
                    ["details"] = new Dictionary<string, object> { ["text"] = message }
                }
            }
        };
    }
}
