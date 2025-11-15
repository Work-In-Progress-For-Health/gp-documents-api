package uk.healthtechwales.gppractice.config;

public enum ProcessingMode {
    /**
     * Synchronous mode: Check binary with ClamAV, respond based on result
     */
    SYNC,
    
    /**
     * Asynchronous mode: Validate syntax/format, respond immediately, 
     * store binary in MinIO and send to RabbitMQ for offline processing
     */
    ASYNC,
    
    /**
     * Hybrid mode: Check binary with ClamAV, respond based on result,
     * if clean store in MinIO and send to RabbitMQ for deeper inspection
     */
    HYBRID
}
