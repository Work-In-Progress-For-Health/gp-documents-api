namespace Uk.HealthTechWales.GpPractice.Services;

public interface IGpPracticeService
{
    Task<bool> IsValidPracticeAsync(string gpPracticeId);
}
