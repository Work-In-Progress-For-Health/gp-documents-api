using Minio;
using Minio.DataModel.Args;

namespace Uk.HealthTechWales.GpPractice.Services;

public class MinIOService : IMinIOService
{
    private readonly ILogger<MinIOService> _logger;
    private readonly IMinioClient _minioClient;
    private readonly string _bucketName;

    public MinIOService(IConfiguration configuration, ILogger<MinIOService> logger)
    {
        _logger = logger;
        var minioUrl = configuration["MinIO:Url"] ?? "http://localhost:9000";
        var accessKey = configuration["MinIO:AccessKey"] ?? "minioadmin";
        var secretKey = configuration["MinIO:SecretKey"] ?? "minioadmin";
        _bucketName = configuration["MinIO:BucketName"] ?? "quarantined";

        _minioClient = new MinioClient()
            .WithEndpoint(minioUrl.Replace("http://", "").Replace("https://", ""))
            .WithCredentials(accessKey, secretKey)
            .WithSSL(minioUrl.StartsWith("https://"))
            .Build();

        _logger.LogInformation("MinIO client initialized for bucket: {BucketName}", _bucketName);
    }

    public async Task<StorageResult> StoreDocumentAsync(string base64Data, string nhsNumber, string? contentType)
    {
        try
        {
            // Decode base64 to bytes
            var documentBytes = Convert.FromBase64String(base64Data);

            // Generate filename: timestamp_nhsnumber
            var timestamp = DateTime.UtcNow.ToString("yyyyMMddHHmmssfff");
            var objectName = $"{timestamp}_{nhsNumber}";

            // Upload to MinIO
            using var stream = new MemoryStream(documentBytes);
            var putObjectArgs = new PutObjectArgs()
                .WithBucket(_bucketName)
                .WithObject(objectName)
                .WithStreamData(stream)
                .WithObjectSize(documentBytes.Length)
                .WithContentType(contentType ?? "application/octet-stream");

            await _minioClient.PutObjectAsync(putObjectArgs);

            _logger.LogInformation("Document stored in MinIO: bucket={BucketName}, object={ObjectName}",
                _bucketName, objectName);

            return StorageResult.CreateSuccess(_bucketName, objectName, contentType, documentBytes.Length);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to store document in MinIO");
            return StorageResult.CreateError($"Failed to store document: {ex.Message}");
        }
    }
}
