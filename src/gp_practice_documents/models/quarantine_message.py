from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class QuarantineMessage(BaseModel):
    """Message sent to RabbitMQ for quarantined documents."""

    object_name: str = Field(..., description="Object name in MinIO")
    bucket_name: str = Field(..., description="Bucket name in MinIO")
    etag: Optional[str] = Field(None, description="ETag from MinIO")
    size: int = Field(..., description="Size of the document in bytes")
    content_type: str = Field(..., description="MIME type of the document")
    submission_id: str = Field(..., description="Unique submission ID")
    patient_id: str = Field(..., description="Patient NHS number")
    document_reference_id: str = Field(..., description="FHIR DocumentReference ID")
    timestamp: datetime = Field(default_factory=datetime.utcnow, description="Timestamp of submission")
    original_filename: str = Field(..., description="Original filename")

    class Config:
        populate_by_name = True
