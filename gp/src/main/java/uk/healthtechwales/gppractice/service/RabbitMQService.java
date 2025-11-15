package uk.healthtechwales.gppractice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.healthtechwales.gppractice.model.QuarantineMessage;

@Service
public class RabbitMQService {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQService.class);

    private final RabbitTemplate rabbitTemplate;
    private final String queueName;
    private final ObjectMapper objectMapper;

    public RabbitMQService(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.queue.name}") String queueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.queueName = queueName;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Send a quarantine message to RabbitMQ
     * @param message the quarantine message
     */
    public void sendQuarantineMessage(QuarantineMessage message) {
        try {
            rabbitTemplate.convertAndSend(queueName, message);
            log.info("Sent message to queue '{}' for Patient ID: {}, Submission ID: {}", 
                    queueName, message.getPatientId(), message.getSubmissionId());
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ queue: {}", queueName, e);
            throw new RuntimeException("Failed to send message to RabbitMQ", e);
        }
    }
}
