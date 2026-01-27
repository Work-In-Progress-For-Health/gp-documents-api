using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Uk.HealthTechWales.GpPractice.Entities;

[Table("gp_practice")]
public class GpPractice
{
    [Key]
    [Column("gp_practice_id")]
    public string GpPracticeId { get; set; } = string.Empty;

    [Column("lhb_code")]
    public string? LhbCode { get; set; }
}
