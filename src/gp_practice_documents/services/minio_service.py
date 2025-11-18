import base64
import logging
from datetime import datetime
from io import BytesIO
from typing import Optional

from minio import Minio
from pydantic import BaseModel

from ..config import Settings

logger = logging.getLogger(__name__)


class StorageResult(BaseModel):
    """Result of storing a document in MinIO."""
    success: bool
    bucket_name: Optional[str] = None
    object_name: Optional[str] = None
    content_type: Optional[str] = None
    size: Optional[int] = None
    error_message: Optional[str] = None


class MinIOService:
    """Service for storing documents in MinIO."""

    def __init__(self, settings: Settings):
        self.bucket_name = settings.minio_bucket_name
        self.client = Minio(
            settings.minio_url,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=False  # Using HTTP as per original config
        )

        logger.info(f"MinIO client initialized for bucket: {self.bucket_name}")

    def store_document(self, base64_data: str, nhs_number: str, content_type: Optional[str] = None) -> StorageResult:
        """
        Store a base64-encoded document in MinIO.

        Args:
            base64_data: Base64-encoded document
            nhs_number: Patient's NHS number
            content_type: MIME type of the document

        Returns:
            StorageResult containing object details
        """
        try:
            # Decode base64 to bytes
            document_bytes = base64.b64decode(base64_data)

            # Generate filename: timestamp_nhsnumber
            timestamp = datetime.utcnow().strftime("%Y%m%d%H%M%S%f")[:-3]  # milliseconds
            object_name = f"{timestamp}_{nhs_number}"

            # Upload to MinIO
            self.client.put_object(
                bucket_name=self.bucket_name,
                object_name=object_name,
                data=BytesIO(document_bytes),
                length=len(document_bytes),
                content_type=content_type or "application/octet-stream"
            )

            logger.info(f"Document stored in MinIO: bucket={self.bucket_name}, object={object_name}")

            return StorageResult(
                success=True,
                bucket_name=self.bucket_name,
                object_name=object_name,
                content_type=content_type,
                size=len(document_bytes)
            )

        except Exception as e:
            logger.error(f"Failed to store document in MinIO: {e}")
            return StorageResult(
                success=False,
                error_message=f"Failed to store document: {str(e)}"
            )
