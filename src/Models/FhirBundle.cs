using System.Text.Json.Serialization;

namespace Uk.HealthTechWales.GpPractice.Models;

public class FhirBundle
{
    [JsonPropertyName("resourceType")]
    public string? ResourceType { get; set; }

    [JsonPropertyName("type")]
    public string? Type { get; set; }

    [JsonPropertyName("entry")]
    public List<FhirEntry>? Entry { get; set; }

    public class FhirEntry
    {
        [JsonPropertyName("resource")]
        public FhirResource? Resource { get; set; }
    }
}
