using Hl7.Fhir.Model;
using Hl7.Fhir.Serialization;
using Uk.HealthTechWales.GpPractice.Models;

namespace Uk.HealthTechWales.GpPractice.Services;

public class FhirValidationService : IFhirValidationService
{
    private readonly FhirJsonParser _parser;
    private readonly ILogger<FhirValidationService> _logger;

    public FhirValidationService(FhirJsonParser parser, ILogger<FhirValidationService> logger)
    {
        _parser = parser;
        _logger = logger;
    }

    public FhirOperationOutcome ValidateBundle(string bundleJson)
    {
        try
        {
            // Parse the bundle - the parser provides structural validation
            // With strict settings (AcceptUnknownMembers=false, AllowUnrecognizedEnums=false)
            var bundle = _parser.Parse<Bundle>(bundleJson);

            // Basic validation checks
            if (bundle == null)
            {
                return FhirOperationOutcome.Error("Bundle parsing resulted in null.");
            }

            if (bundle.Type == null)
            {
                return FhirOperationOutcome.Error("Bundle type is missing.");
            }

            if (bundle.Entry == null || !bundle.Entry.Any())
            {
                return FhirOperationOutcome.Error("Bundle has no entries.");
            }

            _logger.LogInformation("FHIR Bundle validation passed for bundle with {EntryCount} entries", bundle.Entry.Count);
            return FhirOperationOutcome.Success("FHIR Bundle validation passed.");
        }
        catch (FormatException ex)
        {
            _logger.LogError(ex, "FHIR format validation error");
            return FhirOperationOutcome.Error($"FHIR format error: {ex.Message}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "FHIR validation error");
            return FhirOperationOutcome.Error($"FHIR validation error: {ex.Message}");
        }
    }
}
