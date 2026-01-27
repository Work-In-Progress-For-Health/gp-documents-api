using Hl7.Fhir.Model;

namespace Uk.HealthTechWales.GpPractice.Services;

public interface IDocumentProcessingService
{
    Task<ProcessingResult> ProcessDocumentAsync(
        string gpPracticeId,
        Bundle bundle,
        string bundleJson,
        Binary binary,
        string base64Data);
}

public class ProcessingResult
{
    public bool Success { get; set; }
    public string? ScanStatus { get; set; }
    public string? ErrorMessage { get; set; }
    public bool MalwareDetected { get; set; }
    public StorageResult? StorageResult { get; set; }

    public static ProcessingResult CreateSuccess(string scanStatus, StorageResult? storageResult = null)
    {
        return new ProcessingResult
        {
            Success = true,
            ScanStatus = scanStatus,
            StorageResult = storageResult
        };
    }

    public static ProcessingResult CreateError(string errorMessage, bool malwareDetected = false)
    {
        return new ProcessingResult
        {
            Success = false,
            ErrorMessage = errorMessage,
            MalwareDetected = malwareDetected
        };
    }
}
