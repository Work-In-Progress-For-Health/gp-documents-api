package uk.healthtechwales.gppractice.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class MinIOService {

    private static final Logger log = LoggerFactory.getLogger(MinIOService.class);

    private final MinioClient minioClient;
    private final String bucketName;

    public MinIOService(
            @Value("${minio.url}") String minioUrl,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey,
            @Value("${minio.bucket-name}") String bucketName) {
        this.bucketName = bucketName;
        this.minioClient = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
        
        log.info("MinIO client initialized for bucket: {}", bucketName);
    }

    /**
     * Store a base64-encoded document in MinIO
     * @param base64Data the base64-encoded document
     * @param nhsNumber the patient's NHS number
     * @param contentType the content type of the document
     * @return StorageResult containing object details
     */
    public StorageResult storeDocument(String base64Data, String nhsNumber, String contentType) {
        try {
            // Decode base64 to bytes
            byte[] documentBytes = Base64.getDecoder().decode(base64Data);
            
            // Generate filename: timestamp_nhsnumber
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            String objectName = timestamp + "_" + nhsNumber;
            
            // Upload to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(documentBytes), documentBytes.length, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
            
            log.info("Document stored in MinIO: bucket={}, object={}", bucketName, objectName);
            
            return StorageResult.builder()
                    .success(true)
                    .bucketName(bucketName)
                    .objectName(objectName)
                    .contentType(contentType)
                    .size((long) documentBytes.length)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to store document in MinIO", e);
            return StorageResult.builder()
                    .success(false)
                    .errorMessage("Failed to store document: " + e.getMessage())
                    .build();
        }
    }

    public static class StorageResult {
        private boolean success;
        private String bucketName;
        private String objectName;
        private String contentType;
        private Long size;
        private String errorMessage;

        public StorageResult() {}

        private StorageResult(Builder builder) {
            this.success = builder.success;
            this.bucketName = builder.bucketName;
            this.objectName = builder.objectName;
            this.contentType = builder.contentType;
            this.size = builder.size;
            this.errorMessage = builder.errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getBucketName() {
            return bucketName;
        }

        public String getObjectName() {
            return objectName;
        }

        public String getContentType() {
            return contentType;
        }

        public Long getSize() {
            return size;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean success;
            private String bucketName;
            private String objectName;
            private String contentType;
            private Long size;
            private String errorMessage;

            public Builder success(boolean success) {
                this.success = success;
                return this;
            }

            public Builder bucketName(String bucketName) {
                this.bucketName = bucketName;
                return this;
            }

            public Builder objectName(String objectName) {
                this.objectName = objectName;
                return this;
            }

            public Builder contentType(String contentType) {
                this.contentType = contentType;
                return this;
            }

            public Builder size(Long size) {
                this.size = size;
                return this;
            }

            public Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            public StorageResult build() {
                return new StorageResult(this);
            }
        }
    }
}
