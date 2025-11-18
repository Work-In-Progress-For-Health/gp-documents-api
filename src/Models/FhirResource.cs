using System.Text.Json.Serialization;

namespace Uk.HealthTechWales.GpPractice.Models;

public class FhirResource
{
    [JsonPropertyName("resourceType")]
    public string? ResourceType { get; set; }

    [JsonPropertyName("id")]
    public string? Id { get; set; }
}
