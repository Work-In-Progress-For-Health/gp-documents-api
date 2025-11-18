import base64
import logging
import socket
import struct
from enum import Enum
from typing import Optional

from pydantic import BaseModel

from ..config import Settings

logger = logging.getLogger(__name__)


class ScanStatus(str, Enum):
    """ClamAV scan status."""
    CLEAN = "CLEAN"
    INFECTED = "INFECTED"
    ERROR = "ERROR"


class ScanResult(BaseModel):
    """Result of a ClamAV scan."""
    status: ScanStatus
    virus_name: Optional[str] = None

    def is_clean(self) -> bool:
        """Check if the scan result is clean."""
        return self.status == ScanStatus.CLEAN

    def get_details(self) -> str:
        """Get scan result details."""
        if self.status == ScanStatus.CLEAN:
            return "stream: OK"
        elif self.status == ScanStatus.INFECTED:
            return f"stream: {self.virus_name} FOUND"
        else:
            return self.virus_name or "Unknown error"


class ClamAVService:
    """Service for scanning documents with ClamAV."""

    def __init__(self, settings: Settings):
        self.host = settings.clamav_host
        self.port = settings.clamav_port
        self.timeout = settings.clamav_timeout / 1000.0  # Convert ms to seconds

    def scan_document(self, base64_data: str) -> ScanResult:
        """
        Scan a base64-encoded document for malware.

        Args:
            base64_data: Base64-encoded document data

        Returns:
            ScanResult containing scan status and details
        """
        try:
            # Decode base64 to bytes
            document_bytes = base64.b64decode(base64_data)
            return self.scan_file(document_bytes)
        except Exception as e:
            logger.error(f"Error decoding base64 or scanning document: {e}")
            return ScanResult(
                status=ScanStatus.ERROR,
                virus_name=f"Failed to scan document: {str(e)}"
            )

    def scan_file(self, file_data: bytes) -> ScanResult:
        """
        Scan a file for viruses using ClamAV.

        Args:
            file_data: File data to scan

        Returns:
            ScanResult containing scan status and virus name if infected
        """
        logger.info(f"Connecting to ClamAV at {self.host}:{self.port}")

        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.settimeout(self.timeout)
                sock.connect((self.host, self.port))

                # Send INSTREAM command
                sock.sendall(b"zINSTREAM\0")

                # Send file data in chunks
                chunk_size = 2048
                offset = 0

                while offset < len(file_data):
                    length = min(chunk_size, len(file_data) - offset)

                    # Send chunk size (4 bytes, network byte order)
                    sock.sendall(struct.pack("!I", length))

                    # Send chunk data
                    sock.sendall(file_data[offset:offset + length])
                    offset += length

                # Send zero-length chunk to indicate end of stream
                sock.sendall(struct.pack("!I", 0))

                # Read response
                response = sock.recv(4096).decode("utf-8").strip()

                logger.info(f"ClamAV response: {response}")

                return self._parse_response(response)

        except Exception as e:
            logger.error(f"Error connecting to ClamAV: {e}")
            return ScanResult(
                status=ScanStatus.ERROR,
                virus_name=f"Failed to scan file with ClamAV: {str(e)}"
            )

    def _parse_response(self, response: str) -> ScanResult:
        """
        Parse ClamAV response.

        Expected responses:
        - "stream: OK" - file is clean
        - "stream: <virus name> FOUND" - virus detected
        """
        if not response:
            return ScanResult(
                status=ScanStatus.ERROR,
                virus_name="Unknown - empty response from ClamAV"
            )

        # Remove "stream: " prefix if present
        result = response.replace("stream: ", "").strip()

        if result == "OK":
            logger.info("File is clean")
            return ScanResult(status=ScanStatus.CLEAN)
        elif "FOUND" in result:
            # Extract virus name
            virus_name = result.replace(" FOUND", "").strip()
            logger.warning(f"Virus detected: {virus_name}")
            return ScanResult(
                status=ScanStatus.INFECTED,
                virus_name=virus_name
            )
        else:
            logger.error(f"Unexpected ClamAV response: {response}")
            return ScanResult(
                status=ScanStatus.ERROR,
                virus_name=f"Unexpected response: {response}"
            )

    def ping(self) -> bool:
        """Test connection to ClamAV by sending PING command."""
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                sock.settimeout(self.timeout)
                sock.connect((self.host, self.port))

                sock.sendall(b"zPING\0")

                response = sock.recv(4096).decode("utf-8").strip()

                logger.debug(f"ClamAV PING response: {response}")
                return response.replace("\0", "").strip() == "PONG"

        except Exception as e:
            logger.error(f"Failed to ping ClamAV: {e}")
            return False
