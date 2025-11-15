package uk.healthtechwales.gppractice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class QuarantineMessage {
    @JsonProperty("object_name")
    private String objectName;
    
    @JsonProperty("bucket_name")
    private String bucketName;
    
    @JsonProperty("etag")
    private String etag;
    
    @JsonProperty("size")
    private Long size;
    
    @JsonProperty("content_type")
    private String contentType;
    
    @JsonProperty("submission_id")
    private String submissionId;
    
    @JsonProperty("patient_id")
    private String patientId;
    
    @JsonProperty("document_reference_id")
    private String documentReferenceId;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("original_filename")
    private String originalFilename;

    public QuarantineMessage() {
        this.timestamp = Instant.now();
    }

    private QuarantineMessage(Builder builder) {
        this.objectName = builder.objectName;
        this.bucketName = builder.bucketName;
        this.etag = builder.etag;
        this.size = builder.size;
        this.contentType = builder.contentType;
        this.submissionId = builder.submissionId;
        this.patientId = builder.patientId;
        this.documentReferenceId = builder.documentReferenceId;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.originalFilename = builder.originalFilename;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDocumentReferenceId() {
        return documentReferenceId;
    }

    public void setDocumentReferenceId(String documentReferenceId) {
        this.documentReferenceId = documentReferenceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String objectName;
        private String bucketName;
        private String etag;
        private Long size;
        private String contentType;
        private String submissionId;
        private String patientId;
        private String documentReferenceId;
        private Instant timestamp;
        private String originalFilename;

        public Builder objectName(String objectName) {
            this.objectName = objectName;
            return this;
        }

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public Builder etag(String etag) {
            this.etag = etag;
            return this;
        }

        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder submissionId(String submissionId) {
            this.submissionId = submissionId;
            return this;
        }

        public Builder patientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder documentReferenceId(String documentReferenceId) {
            this.documentReferenceId = documentReferenceId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder originalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
            return this;
        }

        public QuarantineMessage build() {
            return new QuarantineMessage(this);
        }
    }
}
