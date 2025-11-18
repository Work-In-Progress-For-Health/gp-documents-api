namespace Uk.HealthTechWales.GpPractice.Services;

public interface IClamAVService
{
    Task<ScanResult> ScanDocumentAsync(string base64Data);
    Task<ScanResult> ScanFileAsync(byte[] fileData);
    Task<bool> PingAsync();
}

public enum ScanStatus
{
    CLEAN,
    INFECTED,
    ERROR
}

public class ScanResult
{
    public ScanStatus Status { get; set; }
    public string? VirusName { get; set; }

    public bool IsClean => Status == ScanStatus.CLEAN;

    public string GetDetails()
    {
        return Status switch
        {
            ScanStatus.CLEAN => "stream: OK",
            ScanStatus.INFECTED => $"stream: {VirusName} FOUND",
            _ => VirusName ?? "Unknown error"
        };
    }

    public static ScanResult CreateClean() => new() { Status = ScanStatus.CLEAN };

    public static ScanResult CreateInfected(string virusName) => new()
    {
        Status = ScanStatus.INFECTED,
        VirusName = virusName
    };

    public static ScanResult CreateError(string message) => new()
    {
        Status = ScanStatus.ERROR,
        VirusName = message
    };
}
