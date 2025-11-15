package uk.healthtechwales.gppractice.service;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.healthtechwales.gppractice.config.ProcessingMode;
import uk.healthtechwales.gppractice.model.QuarantineMessage;

@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final ProcessingMode processingMode;
    private final ClamAVService clamAVService;
    private final MinIOService minioService;
    private final RabbitMQService rabbitMQService;

    public DocumentProcessingService(
            @Value("${document.processing.mode}") String mode,
            ClamAVService clamAVService,
            MinIOService minioService,
            RabbitMQService rabbitMQService) {
        this.processingMode = ProcessingMode.valueOf(mode.toUpperCase());
        this.clamAVService = clamAVService;
        this.minioService = minioService;
        this.rabbitMQService = rabbitMQService;
        
        log.info("Document processing mode: {}", this.processingMode);
    }

    /**
     * Process document based on configured mode
     * @return ProcessingResult
     */
    public ProcessingResult processDocument(
            String gpPracticeId,
            Bundle bundle,
            String bundleJson,
            Binary binary,
            String base64Data) {
        
        switch (processingMode) {
            case SYNC:
                return processSynchronous(base64Data);
                
            case ASYNC:
                return processAsynchronous(gpPracticeId, bundle, bundleJson, binary, base64Data);
                
            case HYBRID:
                return processHybrid(gpPracticeId, bundle, bundleJson, binary, base64Data);
                
            default:
                throw new IllegalStateException("Unknown processing mode: " + processingMode);
        }
    }

    /**
     * SYNC Mode: Check binary with ClamAV, respond based on result
     */
    private ProcessingResult processSynchronous(String base64Data) {
        log.info("Processing in SYNC mode");
        
        ClamAVService.ScanResult scanResult = clamAVService.scanDocument(base64Data);
        
        if (!scanResult.isClean()) {
            return ProcessingResult.builder()
                    .success(false)
                    .errorMessage("Malware detected in submitted document: " + scanResult.getDetails())
                    .malwareDetected(true)
                    .build();
        }
        
        return ProcessingResult.builder()
                .success(true)
                .scanStatus("CLEAN")
                .build();
    }

    /**
     * ASYNC Mode: Validate syntax/format, respond immediately, 
     * store binary in MinIO and send to RabbitMQ for offline processing
     */
    private ProcessingResult processAsynchronous(
            String gpPracticeId,
            Bundle bundle,
            String bundleJson,
            Binary binary,
            String base64Data) {
        
        log.info("Processing in ASYNC mode");
        
        // Extract patient information
        String nhsNumber = extractNHSNumber(bundle);
        String documentReferenceId = extractDocumentReferenceId(bundle);
        
        // Store in MinIO
        MinIOService.StorageResult storageResult = minioService.storeDocument(
                base64Data, 
                nhsNumber, 
                binary.getContentType());
        
        if (!storageResult.isSuccess()) {
            return ProcessingResult.builder()
                    .success(false)
                    .errorMessage("Failed to store document: " + storageResult.getErrorMessage())
                    .build();
        }
        
        // Send to RabbitMQ
        try {
            QuarantineMessage message = QuarantineMessage.builder()
                    .objectName(storageResult.getObjectName())
                    .bucketName(storageResult.getBucketName())
                    .etag(null)
                    .size(storageResult.getSize())
                    .contentType(storageResult.getContentType())
                    .submissionId(java.util.UUID.randomUUID().toString())
                    .patientId(nhsNumber)
                    .documentReferenceId(documentReferenceId)
                    .originalFilename(storageResult.getObjectName())
                    .build();
            
            rabbitMQService.sendQuarantineMessage(message);
            
            return ProcessingResult.builder()
                    .success(true)
                    .scanStatus("PENDING_ASYNC")
                    .storageResult(storageResult)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ", e);
            return ProcessingResult.builder()
                    .success(false)
                    .errorMessage("Failed to queue document for processing: " + e.getMessage())
                    .build();
        }
    }

    /**
     * HYBRID Mode: Check binary with ClamAV, respond based on result,
     * if clean store in MinIO and send to RabbitMQ for deeper inspection
     */
    private ProcessingResult processHybrid(
            String gpPracticeId,
            Bundle bundle,
            String bundleJson,
            Binary binary,
            String base64Data) {
        
        log.info("Processing in HYBRID mode");
        
        // First, scan with ClamAV
        ClamAVService.ScanResult scanResult = clamAVService.scanDocument(base64Data);
        
        if (!scanResult.isClean()) {
            return ProcessingResult.builder()
                    .success(false)
                    .errorMessage("Malware detected in submitted document: " + scanResult.getDetails())
                    .malwareDetected(true)
                    .build();
        }
        
        // Document is clean, now store and queue for deeper inspection
        String nhsNumber = extractNHSNumber(bundle);
        String documentReferenceId = extractDocumentReferenceId(bundle);
        
        // Store in MinIO
        MinIOService.StorageResult storageResult = minioService.storeDocument(
                base64Data, 
                nhsNumber, 
                binary.getContentType());
        
        if (!storageResult.isSuccess()) {
            return ProcessingResult.builder()
                    .success(false)
                    .errorMessage("Failed to store document: " + storageResult.getErrorMessage())
                    .build();
        }
        
        // Send to RabbitMQ for deeper inspection
        try {
            QuarantineMessage message = QuarantineMessage.builder()
                    .objectName(storageResult.getObjectName())
                    .bucketName(storageResult.getBucketName())
                    .etag(null)
                    .size(storageResult.getSize())
                    .contentType(storageResult.getContentType())
                    .submissionId(java.util.UUID.randomUUID().toString())
                    .patientId(nhsNumber)
                    .documentReferenceId(documentReferenceId)
                    .originalFilename(storageResult.getObjectName())
                    .build();
            
            rabbitMQService.sendQuarantineMessage(message);
            
            return ProcessingResult.builder()
                    .success(true)
                    .scanStatus("CLEAN")
                    .storageResult(storageResult)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ", e);
            // Document is still clean and accepted, but couldn't queue for deep scan
            log.warn("Document accepted but not queued for deep inspection");
            return ProcessingResult.builder()
                    .success(true)
                    .scanStatus("CLEAN")
                    .storageResult(storageResult)
                    .build();
        }
    }

    private String extractNHSNumber(Bundle bundle) {
        return bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof org.hl7.fhir.r4.model.Patient)
                .map(entry -> (org.hl7.fhir.r4.model.Patient) entry.getResource())
                .flatMap(patient -> patient.getIdentifier().stream())
                .filter(identifier -> identifier.hasSystem() && 
                        identifier.getSystem().equals("https://fhir.nhs.uk/Id/nhs-number"))
                .map(Identifier::getValue)
                .findFirst()
                .orElse("UNKNOWN");
    }

    private String extractDocumentReferenceId(Bundle bundle) {
        return bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof org.hl7.fhir.r4.model.DocumentReference)
                .map(entry -> (org.hl7.fhir.r4.model.DocumentReference) entry.getResource())
                .filter(docRef -> docRef.hasId())
                .map(docRef -> docRef.getId())
                .findFirst()
                .orElse(java.util.UUID.randomUUID().toString());
    }

    public static class ProcessingResult {
        private boolean success;
        private String scanStatus;
        private String errorMessage;
        private boolean malwareDetected;
        private MinIOService.StorageResult storageResult;

        public ProcessingResult() {}

        private ProcessingResult(Builder builder) {
            this.success = builder.success;
            this.scanStatus = builder.scanStatus;
            this.errorMessage = builder.errorMessage;
            this.malwareDetected = builder.malwareDetected;
            this.storageResult = builder.storageResult;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getScanStatus() {
            return scanStatus;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isMalwareDetected() {
            return malwareDetected;
        }

        public MinIOService.StorageResult getStorageResult() {
            return storageResult;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean success;
            private String scanStatus;
            private String errorMessage;
            private boolean malwareDetected;
            private MinIOService.StorageResult storageResult;

            public Builder success(boolean success) {
                this.success = success;
                return this;
            }

            public Builder scanStatus(String scanStatus) {
                this.scanStatus = scanStatus;
                return this;
            }

            public Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            public Builder malwareDetected(boolean malwareDetected) {
                this.malwareDetected = malwareDetected;
                return this;
            }

            public Builder storageResult(MinIOService.StorageResult storageResult) {
                this.storageResult = storageResult;
                return this;
            }

            public ProcessingResult build() {
                return new ProcessingResult(this);
            }
        }
    }
}
