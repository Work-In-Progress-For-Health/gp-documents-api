using Uk.HealthTechWales.GpPractice.Models;

namespace Uk.HealthTechWales.GpPractice.Services;

public interface IFhirValidationService
{
    FhirOperationOutcome ValidateBundle(string bundleJson);
}
