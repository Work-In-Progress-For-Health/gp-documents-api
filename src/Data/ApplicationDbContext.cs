using Microsoft.EntityFrameworkCore;
using Uk.HealthTechWales.GpPractice.Entities;

namespace Uk.HealthTechWales.GpPractice.Data;

public class ApplicationDbContext : DbContext
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options)
    {
    }

    public DbSet<Entities.GpPractice> GpPractices { get; set; }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Entities.GpPractice>(entity =>
        {
            entity.ToTable("gp_practice");
            entity.HasKey(e => e.GpPracticeId);
            entity.Property(e => e.GpPracticeId).HasColumnName("gp_practice_id");
            entity.Property(e => e.LhbCode).HasColumnName("lhb_code");
        });
    }
}
