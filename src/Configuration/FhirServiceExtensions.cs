using Hl7.Fhir.Model;
using Hl7.Fhir.Specification.Source;
using Hl7.Fhir.Validation;
using Microsoft.Extensions.DependencyInjection;

namespace Uk.HealthTechWales.GpPractice.Configuration;

public static class FhirServiceExtensions
{
    public static IServiceCollection AddFhirServices(this IServiceCollection services)
    {
        // Register FHIR validator as singleton
        services.AddSingleton(provider =>
        {
            var resolver = new CachedResolver(
                new MultiResolver(
                    ZipSource.CreateValidationSource(),
                    new PocoStructureDefinitionSummaryProvider()
                )
            );

            var settings = new ValidationSettings
            {
                ResourceResolver = resolver,
                GenerateSnapshot = true,
                Trace = false,
                ResolveExternalReferences = false
            };

            return new Validator(settings);
        });

        return services;
    }
}
