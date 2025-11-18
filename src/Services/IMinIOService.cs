namespace Uk.HealthTechWales.GpPractice.Services;

public interface IMinIOService
{
    Task<StorageResult> StoreDocumentAsync(string base64Data, string nhsNumber, string? contentType);
}

public class StorageResult
{
    public bool Success { get; set; }
    public string? BucketName { get; set; }
    public string? ObjectName { get; set; }
    public string? ContentType { get; set; }
    public long? Size { get; set; }
    public string? ErrorMessage { get; set; }

    public static StorageResult CreateSuccess(string bucketName, string objectName, string? contentType, long size)
    {
        return new StorageResult
        {
            Success = true,
            BucketName = bucketName,
            ObjectName = objectName,
            ContentType = contentType,
            Size = size
        };
    }

    public static StorageResult CreateError(string errorMessage)
    {
        return new StorageResult
        {
            Success = false,
            ErrorMessage = errorMessage
        };
    }
}
