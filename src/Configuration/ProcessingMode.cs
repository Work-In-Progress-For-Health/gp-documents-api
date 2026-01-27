namespace Uk.HealthTechWales.GpPractice.Configuration;

/// <summary>
/// Document processing mode configuration
/// </summary>
public enum ProcessingMode
{
    /// <summary>
    /// Synchronous mode: Check binary with ClamAV, respond based on result
    /// </summary>
    SYNC,

    /// <summary>
    /// Asynchronous mode: Validate syntax/format, respond immediately,
    /// store binary in MinIO and send to RabbitMQ for offline processing
    /// </summary>
    ASYNC,

    /// <summary>
    /// Hybrid mode: Check binary with ClamAV, respond based on result,
    /// if clean store in MinIO and send to RabbitMQ for deeper inspection
    /// </summary>
    HYBRID
}
