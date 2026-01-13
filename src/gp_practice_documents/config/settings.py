from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

from .processing_mode import ProcessingMode


class Settings(BaseSettings):
    """Application settings loaded from environment variables or .env file."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore"
    )

    # Server
    server_port: int = Field(default=8080, alias="SERVER_PORT")

    # SQL Server
    database_url: str = Field(
        default="mssql+pyodbc://sa:Hc4u!1peter5v7@mssql.mssql.svc.cluster.local:1433/gp_practices"
        "?driver=ODBC+Driver+18+for+SQL+Server&TrustServerCertificate=yes&Encrypt=no",
        alias="DATABASE_URL"
    )

    # Document Processing Mode
    processing_mode: ProcessingMode = Field(default=ProcessingMode.HYBRID, alias="PROCESSING_MODE")

    # ClamAV Configuration
    clamav_host: str = Field(default="clamav.malware-check.svc.cluster.local", alias="CLAMAV_HOST")
    clamav_port: int = Field(default=3310, alias="CLAMAV_PORT")
    clamav_timeout: int = Field(default=5000, alias="CLAMAV_TIMEOUT")

    # MinIO Configuration
    minio_url: str = Field(default="minio.malware-check.svc.cluster.local:9000", alias="MINIO_URL")
    minio_access_key: str = Field(default="minioadmin", alias="MINIO_ACCESS_KEY")
    minio_secret_key: str = Field(default="minioadmin123", alias="MINIO_SECRET_KEY")
    minio_bucket_name: str = Field(default="quarantined", alias="MINIO_BUCKET_NAME")

    # RabbitMQ Configuration
    rabbitmq_host: str = Field(
        default="production-rabbitmqcluster.default.svc.cluster.local",
        alias="RABBITMQ_HOST"
    )
    rabbitmq_port: int = Field(default=5672, alias="RABBITMQ_PORT")
    rabbitmq_username: str = Field(default="guest", alias="RABBITMQ_USERNAME")
    rabbitmq_password: str = Field(default="guest", alias="RABBITMQ_PASSWORD")
    rabbitmq_queue_name: str = Field(default="quarantined", alias="RABBITMQ_QUEUE_NAME")

    # Logging
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")


@lru_cache
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()
