using System.Text.Json.Serialization;

namespace Uk.HealthTechWales.GpPractice.Models;

public class QuarantineMessage
{
    [JsonPropertyName("object_name")]
    public string? ObjectName { get; set; }

    [JsonPropertyName("bucket_name")]
    public string? BucketName { get; set; }

    [JsonPropertyName("etag")]
    public string? Etag { get; set; }

    [JsonPropertyName("size")]
    public long? Size { get; set; }

    [JsonPropertyName("content_type")]
    public string? ContentType { get; set; }

    [JsonPropertyName("submission_id")]
    public string? SubmissionId { get; set; }

    [JsonPropertyName("patient_id")]
    public string? PatientId { get; set; }

    [JsonPropertyName("document_reference_id")]
    public string? DocumentReferenceId { get; set; }

    [JsonPropertyName("timestamp")]
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;

    [JsonPropertyName("original_filename")]
    public string? OriginalFilename { get; set; }

    public class Builder
    {
        private readonly QuarantineMessage _message = new();

        public Builder ObjectName(string objectName)
        {
            _message.ObjectName = objectName;
            return this;
        }

        public Builder BucketName(string bucketName)
        {
            _message.BucketName = bucketName;
            return this;
        }

        public Builder Etag(string? etag)
        {
            _message.Etag = etag;
            return this;
        }

        public Builder Size(long size)
        {
            _message.Size = size;
            return this;
        }

        public Builder ContentType(string contentType)
        {
            _message.ContentType = contentType;
            return this;
        }

        public Builder SubmissionId(string submissionId)
        {
            _message.SubmissionId = submissionId;
            return this;
        }

        public Builder PatientId(string patientId)
        {
            _message.PatientId = patientId;
            return this;
        }

        public Builder DocumentReferenceId(string documentReferenceId)
        {
            _message.DocumentReferenceId = documentReferenceId;
            return this;
        }

        public Builder Timestamp(DateTime timestamp)
        {
            _message.Timestamp = timestamp;
            return this;
        }

        public Builder OriginalFilename(string originalFilename)
        {
            _message.OriginalFilename = originalFilename;
            return this;
        }

        public QuarantineMessage Build() => _message;
    }

    public static Builder CreateBuilder() => new();
}
