using Hl7.Fhir.Serialization;
using Microsoft.Extensions.DependencyInjection;

namespace Uk.HealthTechWales.GpPractice.Configuration;

public static class FhirServiceExtensions
{
    public static IServiceCollection AddFhirServices(this IServiceCollection services)
    {
        // Register FHIR parser with strict validation settings
        services.AddSingleton(provider =>
        {
            var settings = new ParserSettings
            {
                AcceptUnknownMembers = false,
                AllowUnrecognizedEnums = false,
                PermissiveParsing = false
            };

            return new FhirJsonParser(settings);
        });

        return services;
    }
}
