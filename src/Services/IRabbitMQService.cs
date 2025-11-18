using Uk.HealthTechWales.GpPractice.Models;

namespace Uk.HealthTechWales.GpPractice.Services;

public interface IRabbitMQService
{
    void SendQuarantineMessage(QuarantineMessage message);
}
