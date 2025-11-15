package uk.healthtechwales.gppractice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class ClamAVService {

    private static final Logger log = LoggerFactory.getLogger(ClamAVService.class);

    @Value("${clamav.host}")
    private String clamAvHost;

    @Value("${clamav.port}")
    private int clamAvPort;

    @Value("${clamav.timeout:5000}")
    private int timeout;

    /**
     * Scans a base64-encoded document for malware
     * @param base64Data the base64-encoded document
     * @return ScanResult containing scan status and details
     */
    public ScanResult scanDocument(String base64Data) {
        try {
            // Decode base64 to bytes
            byte[] documentBytes = Base64.getDecoder().decode(base64Data);
            return scanFile(documentBytes);
        } catch (Exception e) {
            log.error("Error decoding base64 or scanning document", e);
            return ScanResult.builder()
                    .status(ScanStatus.ERROR)
                    .virusName("Failed to scan document: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Scan a file for viruses using ClamAV
     * @param fileData the file data to scan
     * @return ScanResult containing scan status and virus name if infected
     */
    public ScanResult scanFile(byte[] fileData) throws IOException {
        log.info("Connecting to ClamAV at {}:{}", clamAvHost, clamAvPort);
        
        try (Socket socket = new Socket(clamAvHost, clamAvPort)) {
            socket.setSoTimeout(timeout);
            
            try (OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {
                
                // Send INSTREAM command
                out.write("zINSTREAM\0".getBytes(StandardCharsets.UTF_8));
                out.flush();
                
                // Send file data in chunks
                int chunkSize = 2048;
                int offset = 0;
                
                while (offset < fileData.length) {
                    int length = Math.min(chunkSize, fileData.length - offset);
                    
                    // Send chunk size (4 bytes, network byte order)
                    byte[] sizeBytes = ByteBuffer.allocate(4).putInt(length).array();
                    out.write(sizeBytes);
                    
                    // Send chunk data
                    out.write(fileData, offset, length);
                    offset += length;
                }
                
                // Send zero-length chunk to indicate end of stream
                out.write(new byte[]{0, 0, 0, 0});
                out.flush();
                
                // Read response
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String response = reader.readLine();
                
                log.info("ClamAV response: {}", response);
                
                return parseResponse(response);
            }
        } catch (IOException e) {
            log.error("Error connecting to ClamAV", e);
            return ScanResult.builder()
                    .status(ScanStatus.ERROR)
                    .virusName("Failed to scan file with ClamAV: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Parse ClamAV response
     * Expected responses:
     * - "stream: OK" - file is clean
     * - "stream: <virus name> FOUND" - virus detected
     */
    private ScanResult parseResponse(String response) {
        if (response == null || response.isEmpty()) {
            return ScanResult.builder()
                    .status(ScanStatus.ERROR)
                    .virusName("Unknown - empty response from ClamAV")
                    .build();
        }

        // Remove "stream: " prefix if present
        String result = response.replace("stream: ", "").trim();

        if (result.equals("OK")) {
            log.info("File is clean");
            return ScanResult.builder()
                    .status(ScanStatus.CLEAN)
                    .build();
        } else if (result.contains("FOUND")) {
            // Extract virus name
            String virusName = result.replace(" FOUND", "").trim();
            log.warn("Virus detected: {}", virusName);
            return ScanResult.builder()
                    .status(ScanStatus.INFECTED)
                    .virusName(virusName)
                    .build();
        } else {
            log.error("Unexpected ClamAV response: {}", response);
            return ScanResult.builder()
                    .status(ScanStatus.ERROR)
                    .virusName("Unexpected response: " + response)
                    .build();
        }
    }

    /**
     * Test connection to ClamAV by sending PING command
     */
    public boolean ping() {
        try (Socket socket = new Socket(clamAvHost, clamAvPort)) {
            socket.setSoTimeout(timeout);
            
            try (OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {
                
                out.write("zPING\0".getBytes(StandardCharsets.UTF_8));
                out.flush();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String response = reader.readLine();
                
                log.debug("ClamAV PING response: {}", response);
                return "PONG".equals(response.replace("\0", "").trim());
            }
        } catch (IOException e) {
            log.error("Failed to ping ClamAV", e);
            return false;
        }
    }

    public enum ScanStatus {
        CLEAN,
        INFECTED,
        ERROR
    }

    public static class ScanResult {
        private ScanStatus status;
        private String virusName;

        public ScanResult() {}

        private ScanResult(Builder builder) {
            this.status = builder.status;
            this.virusName = builder.virusName;
        }

        public ScanStatus getStatus() {
            return status;
        }

        public void setStatus(ScanStatus status) {
            this.status = status;
        }

        public String getVirusName() {
            return virusName;
        }

        public void setVirusName(String virusName) {
            this.virusName = virusName;
        }

        public boolean isClean() {
            return status == ScanStatus.CLEAN;
        }

        public String getDetails() {
            if (status == ScanStatus.CLEAN) {
                return "stream: OK";
            } else if (status == ScanStatus.INFECTED) {
                return "stream: " + virusName + " FOUND";
            } else {
                return virusName != null ? virusName : "Unknown error";
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ScanStatus status;
            private String virusName;

            public Builder status(ScanStatus status) {
                this.status = status;
                return this;
            }

            public Builder virusName(String virusName) {
                this.virusName = virusName;
                return this;
            }

            public ScanResult build() {
                return new ScanResult(this);
            }
        }
    }
}
