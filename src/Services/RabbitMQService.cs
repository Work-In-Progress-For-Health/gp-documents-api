using System.Text;
using System.Text.Json;
using RabbitMQ.Client;
using Uk.HealthTechWales.GpPractice.Models;

namespace Uk.HealthTechWales.GpPractice.Services;

public class RabbitMQService : IRabbitMQService
{
    private readonly ILogger<RabbitMQService> _logger;
    private readonly IModel _channel;
    private readonly string _queueName;
    private readonly JsonSerializerOptions _jsonOptions;

    public RabbitMQService(IConfiguration configuration, IModel channel, ILogger<RabbitMQService> logger)
    {
        _logger = logger;
        _channel = channel;
        _queueName = configuration["RabbitMQ:QueueName"] ?? "quarantined";

        _jsonOptions = new JsonSerializerOptions
        {
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
            WriteIndented = false
        };
    }

    public void SendQuarantineMessage(QuarantineMessage message)
    {
        try
        {
            var json = JsonSerializer.Serialize(message, _jsonOptions);
            var body = Encoding.UTF8.GetBytes(json);

            var properties = _channel.CreateBasicProperties();
            properties.Persistent = true;
            properties.ContentType = "application/json";

            _channel.BasicPublish(
                exchange: "",
                routingKey: _queueName,
                basicProperties: properties,
                body: body
            );

            _logger.LogInformation(
                "Sent message to queue '{QueueName}' for Patient ID: {PatientId}, Submission ID: {SubmissionId}",
                _queueName, message.PatientId, message.SubmissionId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send message to RabbitMQ queue: {QueueName}", _queueName);
            throw new InvalidOperationException("Failed to send message to RabbitMQ", ex);
        }
    }
}
