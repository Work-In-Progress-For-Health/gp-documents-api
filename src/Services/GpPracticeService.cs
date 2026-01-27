using Microsoft.EntityFrameworkCore;
using Uk.HealthTechWales.GpPractice.Data;

namespace Uk.HealthTechWales.GpPractice.Services;

public class GpPracticeService : IGpPracticeService
{
    private readonly ApplicationDbContext _context;

    public GpPracticeService(ApplicationDbContext context)
    {
        _context = context;
    }

    public async Task<bool> IsValidPracticeAsync(string gpPracticeId)
    {
        return await _context.GpPractices
            .AnyAsync(gp => gp.GpPracticeId == gpPracticeId);
    }
}
