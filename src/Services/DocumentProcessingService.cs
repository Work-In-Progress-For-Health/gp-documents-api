using Hl7.Fhir.Model;
using Uk.HealthTechWales.GpPractice.Configuration;
using Uk.HealthTechWales.GpPractice.Models;

namespace Uk.HealthTechWales.GpPractice.Services;

public class DocumentProcessingService : IDocumentProcessingService
{
    private readonly ILogger<DocumentProcessingService> _logger;
    private readonly ProcessingMode _processingMode;
    private readonly IClamAVService _clamAvService;
    private readonly MinIOService _minioService;
    private readonly IRabbitMQService _rabbitMqService;

    public DocumentProcessingService(
        IConfiguration configuration,
        IClamAVService clamAvService,
        MinIOService minioService,
        IRabbitMQService rabbitMqService,
        ILogger<DocumentProcessingService> logger)
    {
        _logger = logger;
        var mode = configuration["DocumentProcessing:Mode"] ?? "HYBRID";
        _processingMode = Enum.Parse<ProcessingMode>(mode, ignoreCase: true);
        _clamAvService = clamAvService;
        _minioService = minioService;
        _rabbitMqService = rabbitMqService;

        _logger.LogInformation("Document processing mode: {ProcessingMode}", _processingMode);
    }

    public async Task<ProcessingResult> ProcessDocumentAsync(
        string gpPracticeId,
        Bundle bundle,
        string bundleJson,
        Binary binary,
        string base64Data)
    {
        return _processingMode switch
        {
            ProcessingMode.SYNC => await ProcessSynchronousAsync(base64Data),
            ProcessingMode.ASYNC => await ProcessAsynchronousAsync(gpPracticeId, bundle, bundleJson, binary, base64Data),
            ProcessingMode.HYBRID => await ProcessHybridAsync(gpPracticeId, bundle, bundleJson, binary, base64Data),
            _ => throw new InvalidOperationException($"Unknown processing mode: {_processingMode}")
        };
    }

    private async Task<ProcessingResult> ProcessSynchronousAsync(string base64Data)
    {
        _logger.LogInformation("Processing in SYNC mode");

        var scanResult = await _clamAvService.ScanDocumentAsync(base64Data);

        if (!scanResult.IsClean)
        {
            return ProcessingResult.CreateError(
                $"Malware detected in submitted document: {scanResult.GetDetails()}",
                malwareDetected: true);
        }

        return ProcessingResult.CreateSuccess("CLEAN");
    }

    private async Task<ProcessingResult> ProcessAsynchronousAsync(
        string gpPracticeId,
        Bundle bundle,
        string bundleJson,
        Binary binary,
        string base64Data)
    {
        _logger.LogInformation("Processing in ASYNC mode");

        // Extract patient information
        var nhsNumber = ExtractNHSNumber(bundle);
        var documentReferenceId = ExtractDocumentReferenceId(bundle);

        // Store in MinIO
        var storageResult = await _minioService.StoreDocumentAsync(
            base64Data,
            nhsNumber,
            binary.ContentType);

        if (!storageResult.Success)
        {
            return ProcessingResult.CreateError($"Failed to store document: {storageResult.ErrorMessage}");
        }

        // Send to RabbitMQ
        try
        {
            var message = QuarantineMessage.CreateBuilder()
                .ObjectName(storageResult.ObjectName!)
                .BucketName(storageResult.BucketName!)
                .Etag(null)
                .Size(storageResult.Size!.Value)
                .ContentType(storageResult.ContentType)
                .SubmissionId(Guid.NewGuid().ToString())
                .PatientId(nhsNumber)
                .DocumentReferenceId(documentReferenceId)
                .OriginalFilename(storageResult.ObjectName!)
                .Build();

            _rabbitMqService.SendQuarantineMessage(message);

            return ProcessingResult.CreateSuccess("PENDING_ASYNC", storageResult);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send message to RabbitMQ");
            return ProcessingResult.CreateError($"Failed to queue document for processing: {ex.Message}");
        }
    }

    private async Task<ProcessingResult> ProcessHybridAsync(
        string gpPracticeId,
        Bundle bundle,
        string bundleJson,
        Binary binary,
        string base64Data)
    {
        _logger.LogInformation("Processing in HYBRID mode");

        // First, scan with ClamAV
        var scanResult = await _clamAvService.ScanDocumentAsync(base64Data);

        if (!scanResult.IsClean)
        {
            return ProcessingResult.CreateError(
                $"Malware detected in submitted document: {scanResult.GetDetails()}",
                malwareDetected: true);
        }

        // Document is clean, now store and queue for deeper inspection
        var nhsNumber = ExtractNHSNumber(bundle);
        var documentReferenceId = ExtractDocumentReferenceId(bundle);

        // Store in MinIO
        var storageResult = await _minioService.StoreDocumentAsync(
            base64Data,
            nhsNumber,
            binary.ContentType);

        if (!storageResult.Success)
        {
            return ProcessingResult.CreateError($"Failed to store document: {storageResult.ErrorMessage}");
        }

        // Send to RabbitMQ for deeper inspection
        try
        {
            var message = QuarantineMessage.CreateBuilder()
                .ObjectName(storageResult.ObjectName!)
                .BucketName(storageResult.BucketName!)
                .Etag(null)
                .Size(storageResult.Size!.Value)
                .ContentType(storageResult.ContentType)
                .SubmissionId(Guid.NewGuid().ToString())
                .PatientId(nhsNumber)
                .DocumentReferenceId(documentReferenceId)
                .OriginalFilename(storageResult.ObjectName!)
                .Build();

            _rabbitMqService.SendQuarantineMessage(message);

            return ProcessingResult.CreateSuccess("CLEAN", storageResult);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send message to RabbitMQ");
            // Document is still clean and accepted, but couldn't queue for deep scan
            _logger.LogWarning("Document accepted but not queued for deep inspection");
            return ProcessingResult.CreateSuccess("CLEAN", storageResult);
        }
    }

    private string ExtractNHSNumber(Bundle bundle)
    {
        var patient = bundle.Entry?
            .Select(e => e.Resource)
            .OfType<Patient>()
            .FirstOrDefault();

        if (patient == null) return "UNKNOWN";

        var nhsNumber = patient.Identifier?
            .FirstOrDefault(id => id.System == "https://fhir.nhs.uk/Id/nhs-number")
            ?.Value;

        return nhsNumber ?? "UNKNOWN";
    }

    private string ExtractDocumentReferenceId(Bundle bundle)
    {
        var docRef = bundle.Entry?
            .Select(e => e.Resource)
            .OfType<DocumentReference>()
            .FirstOrDefault();

        return docRef?.Id ?? Guid.NewGuid().ToString();
    }
}
