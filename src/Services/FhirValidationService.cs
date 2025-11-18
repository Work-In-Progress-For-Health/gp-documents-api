using Hl7.Fhir.Model;
using Hl7.Fhir.Serialization;
using Hl7.Fhir.Validation;
using Uk.HealthTechWales.GpPractice.Models;

namespace Uk.HealthTechWales.GpPractice.Services;

public class FhirValidationService : IFhirValidationService
{
    private readonly Validator _validator;
    private readonly FhirJsonParser _parser;

    public FhirValidationService(Validator validator)
    {
        _validator = validator;
        _parser = new FhirJsonParser();
    }

    public FhirOperationOutcome ValidateBundle(string bundleJson)
    {
        try
        {
            var bundle = _parser.Parse<Bundle>(bundleJson);
            var result = _validator.Validate(bundle);

            if (result.IsSuccessful)
            {
                return FhirOperationOutcome.Success("FHIR Bundle validation passed.");
            }

            var outcome = result.ToOperationOutcome();
            var details = string.Join("; ",
                outcome.Issue.Select(i => $"{i.Severity}: {i.Diagnostics}"));

            return FhirOperationOutcome.Error($"FHIR validation failed: {details}");
        }
        catch (Exception ex)
        {
            return FhirOperationOutcome.Error($"FHIR validation error: {ex.Message}");
        }
    }
}
