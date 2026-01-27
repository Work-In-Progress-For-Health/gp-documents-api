using Microsoft.EntityFrameworkCore;
using Serilog;
using Uk.HealthTechWales.GpPractice.Configuration;
using Uk.HealthTechWales.GpPractice.Data;
using Uk.HealthTechWales.GpPractice.Services;

var builder = WebApplication.CreateBuilder(args);

// Configure Serilog
Log.Logger = new LoggerConfiguration()
    .ReadFrom.Configuration(builder.Configuration)
    .WriteTo.Console()
    .CreateLogger();

builder.Host.UseSerilog();

// Add services to the container
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase;
        options.JsonSerializerOptions.WriteIndented = false;
    });

// Configure Database
builder.Services.AddDbContext<ApplicationDbContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));

// Configure FHIR
builder.Services.AddFhirServices();

// Configure RabbitMQ
builder.Services.AddRabbitMqServices(builder.Configuration);

// Configure MinIO
builder.Services.AddSingleton<MinIOService>();

// Add application services
builder.Services.AddScoped<IClamAVService, ClamAVService>();
builder.Services.AddScoped<IGpPracticeService, GpPracticeService>();
builder.Services.AddScoped<IDocumentProcessingService, DocumentProcessingService>();
builder.Services.AddScoped<IFhirValidationService, FhirValidationService>();
builder.Services.AddSingleton<IRabbitMQService, RabbitMQService>();

// Configure Swagger/OpenAPI
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Configure CORS if needed
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader();
    });
});

var app = builder.Build();

// Configure the HTTP request pipeline
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseSerilogRequestLogging();

app.UseRouting();

app.UseCors();

app.UseAuthorization();

app.MapControllers();

try
{
    Log.Information("Starting GP Practice Documents API");
    app.Run();
}
catch (Exception ex)
{
    Log.Fatal(ex, "Application terminated unexpectedly");
}
finally
{
    Log.CloseAndFlush();
}
