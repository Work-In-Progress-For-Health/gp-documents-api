# GP Practice Documents API - Dev Container

This devcontainer provides a complete development environment for the GP Practice Documents API with all required services.

## Features

### Development Stack
- **.NET 8 SDK** - Latest .NET development tools
- **Entity Framework Core Tools** - Database migrations and scaffolding
- **SQL Server 2022** - Production-like database
- **RabbitMQ** - Message queue with management UI
- **MinIO** - S3-compatible object storage
- **ClamAV** - Virus scanning service

### VS Code Extensions Included

**C# Development:**
- C# Dev Kit
- C# Language Support
- .NET Runtime

**Database:**
- SQL Server (mssql)
- Data Workspace

**API Testing:**
- REST Client
- Thunder Client

**Docker & DevOps:**
- Docker
- YAML Support

**Code Quality:**
- GitLens
- EditorConfig
- Error Lens
- Better Comments
- Code Spell Checker

**Productivity:**
- Todo Tree
- Markdown All in One
- Material Icon Theme

## Getting Started

### Prerequisites
- [VS Code](https://code.visualstudio.com/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop)
- [Dev Containers Extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)

### Opening the Project

1. Open VS Code
2. Open the repository folder
3. When prompted, click "Reopen in Container" or use Command Palette: `Dev Containers: Reopen in Container`
4. Wait for the container to build and start (first time may take several minutes)

### Services Access

Once the container is running, you can access:

| Service | URL | Credentials |
|---------|-----|-------------|
| API | http://localhost:8080 | N/A |
| SQL Server | localhost:1433 | User: `sa`, Password: `Hc4u!1peter5v7` |
| RabbitMQ Management | http://localhost:15672 | User: `guest`, Password: `guest` |
| MinIO Console | http://localhost:9001 | User: `minioadmin`, Password: `minioadmin123` |
| MinIO API | http://localhost:9000 | N/A |

### Database Setup

The SQL Server database requires initial setup:

```bash
# Connect using sqlcmd (already in PATH)
sqlcmd -S localhost -U sa -P 'Hc4u!1peter5v7' -C

# Create database
CREATE DATABASE gp_practices;
GO

# Run migrations (if using EF Core migrations)
cd src
dotnet ef database update
```

### MinIO Bucket Setup

Create the required bucket for quarantined files:

```bash
# Using MinIO Client (mc) or via the web console at http://localhost:9001
# Login with minioadmin/minioadmin123 and create bucket "quarantined"
```

### Running the Application

```bash
# Restore dependencies
cd src
dotnet restore

# Build the project
dotnet build

# Run the application
dotnet run
```

The API will be available at http://localhost:8080

### Running with Watch Mode

For development with hot reload:

```bash
cd src
dotnet watch run
```

### Running Tests

```bash
cd src
dotnet test
```

## Development Workflow

### Code Formatting

The workspace is configured to format on save. You can also manually format:

```bash
dotnet format
```

### Database Migrations

```bash
# Add a new migration
dotnet ef migrations add MigrationName

# Update database
dotnet ef database update

# Remove last migration
dotnet ef migrations remove
```

### NuGet Package Management

- Use the NuGet Package Manager extension in VS Code
- Or use the command line:

```bash
# Add a package
dotnet add package PackageName

# Update packages
dotnet restore
```

## Troubleshooting

### Services Not Starting

Check service health:

```bash
# Check SQL Server
docker ps | grep sqlserver

# Check RabbitMQ
docker ps | grep rabbitmq

# View logs
docker logs <container-id>
```

### Port Conflicts

If ports are already in use, you can modify the ports in `.devcontainer/docker-compose.yml`

### Slow Performance

- Ensure Docker Desktop has adequate resources (CPU: 4+ cores, Memory: 8+ GB)
- Check disk space
- Consider using named volumes for better performance

## Environment Variables

The following environment variables are pre-configured:

- `ASPNETCORE_ENVIRONMENT=Development`
- `DOTNET_CLI_TELEMETRY_OPTOUT=1`

Additional variables can be set in `.devcontainer/devcontainer.json` under `remoteEnv`.

## Useful Commands

```bash
# Check .NET version
dotnet --version

# List installed tools
dotnet tool list --global

# SQL Server status
sqlcmd -S localhost -U sa -P 'Hc4u!1peter5v7' -Q "SELECT @@VERSION" -C

# Check outdated packages
dotnet outdated

# Entity Framework tools
dotnet ef --version
```

## Extensions Configuration

All extensions are automatically installed when the container starts. Settings are pre-configured for:

- C# code formatting
- SQL Server connections
- Editor preferences
- Git integration

## Support

For issues or questions about the dev container setup, please check:
- [Dev Containers Documentation](https://code.visualstudio.com/docs/devcontainers/containers)
- [Docker Documentation](https://docs.docker.com/)
- [.NET Documentation](https://docs.microsoft.com/dotnet/)
