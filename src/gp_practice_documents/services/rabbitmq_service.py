import json
import logging

import pika

from ..config import Settings
from ..models import QuarantineMessage

logger = logging.getLogger(__name__)


class RabbitMQService:
    """Service for sending messages to RabbitMQ."""

    def __init__(self, settings: Settings):
        self.queue_name = settings.rabbitmq_queue_name
        self.credentials = pika.PlainCredentials(
            settings.rabbitmq_username,
            settings.rabbitmq_password
        )
        self.connection_params = pika.ConnectionParameters(
            host=settings.rabbitmq_host,
            port=settings.rabbitmq_port,
            credentials=self.credentials
        )

    def send_quarantine_message(self, message: QuarantineMessage) -> None:
        """
        Send a quarantine message to RabbitMQ.

        Args:
            message: The quarantine message to send

        Raises:
            RuntimeError: If failed to send message
        """
        try:
            connection = pika.BlockingConnection(self.connection_params)
            channel = connection.channel()

            # Declare queue (idempotent)
            channel.queue_declare(queue=self.queue_name, durable=True)

            # Convert message to JSON
            message_body = message.model_dump_json()

            # Publish message
            channel.basic_publish(
                exchange="",
                routing_key=self.queue_name,
                body=message_body,
                properties=pika.BasicProperties(
                    delivery_mode=2,  # Make message persistent
                    content_type="application/json"
                )
            )

            logger.info(
                f"Sent message to queue '{self.queue_name}' for Patient ID: {message.patient_id}, "
                f"Submission ID: {message.submission_id}"
            )

            connection.close()

        except Exception as e:
            logger.error(f"Failed to send message to RabbitMQ queue: {self.queue_name}", exc_info=e)
            raise RuntimeError(f"Failed to send message to RabbitMQ: {str(e)}")
