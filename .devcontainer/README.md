# DevContainer Configuration

This devcontainer provides a complete development environment for the GP Practice Documents API.

## What's Included

### Base Environment
- Python 3.11 (Debian Bullseye)
- Git and GitHub CLI
- Docker-in-Docker support

### Python Tools
- **uv** - Fast Python package manager
- **just** - Command runner for common tasks
- **ruff** - Fast Python linter and formatter
- **pytest** - Testing framework

### VS Code Extensions

#### Python Development
- **Python** - Core Python support
- **Pylance** - Advanced type checking and IntelliSense
- **Black Formatter** - Code formatting
- **Ruff** - Fast linting and formatting
- **Debugpy** - Python debugging

#### API Development & Testing
- **Thunder Client** - REST API client for testing
- **REST Client** - Alternative HTTP client

#### Database
- **SQLTools** - Database management and queries
- **SQLTools MS SQL Driver** - MS SQL Server support

#### Configuration Files
- **Even Better TOML** - Enhanced TOML support
- **YAML** - YAML language support
- **DotEnv** - .env file support

#### Task Running
- **Just Syntax** - Syntax highlighting for justfiles

#### Code Quality
- **Error Lens** - Inline error highlighting
- **Code Spell Checker** - Spell checking
- **autoDocstring** - Automatic docstring generation
- **IntelliCode** - AI-assisted development

#### Git
- **GitLens** - Enhanced Git capabilities
- **Git Graph** - Visual Git history

#### Productivity
- **Path IntelliSense** - Autocomplete for file paths
- **Markdown All in One** - Markdown support
- **Docker** - Docker support

## Getting Started

### Prerequisites
- [VS Code](https://code.visualstudio.com/)
- [Remote - Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)
- [Docker Desktop](https://www.docker.com/products/docker-desktop)

### Opening the Project

1. Open VS Code
2. Open the command palette (F1 or Ctrl+Shift+P)
3. Select "Remote-Containers: Open Folder in Container..."
4. Select the project folder
5. Wait for the container to build and start (first time takes a few minutes)

### After Container Starts

The devcontainer will automatically:
1. Install `uv` and `just`
2. Install MS SQL Server ODBC drivers
3. Install Python dependencies
4. Create a `.env` file from `.env.example` if it doesn't exist

### Using the Environment

#### Run the application in development mode:
```bash
just dev
```

#### Access the API:
- API: http://localhost:8080
- Interactive docs: http://localhost:8080/docs
- Health check: http://localhost:8080/health

#### Run tests:
```bash
just test
```

#### Format code:
```bash
just format
```

#### Lint code:
```bash
just lint
```

#### See all available commands:
```bash
just --list
```

## VS Code Settings

The devcontainer configures VS Code with optimized settings for Python development:

- **Format on save** enabled
- **Auto-organize imports** on save
- **Ruff** as default formatter
- **120 character line length**
- **PyTest** configured for testing
- **Type checking** enabled (basic mode)
- **Inlay hints** for types and return values

## Database Configuration

The devcontainer includes SQLTools with MS SQL Server driver. To connect to your database:

1. Install the SQLTools extension (included)
2. Open the SQLTools panel (database icon in sidebar)
3. Add a new connection with your database credentials

Example connection:
- **Server**: mssql.mssql.svc.cluster.local
- **Port**: 1433
- **Database**: gp_practices
- **Username**: sa
- **Password**: (from .env file)

## Testing APIs

Use Thunder Client or REST Client extensions to test the API:

### Thunder Client (GUI)
1. Open Thunder Client from sidebar
2. Create a new request
3. Set URL to http://localhost:8080/api/v1/gp-practice/{id}/documents
4. Add request body and headers

### REST Client (File-based)
Create a `.http` file:
```http
### Health Check
GET http://localhost:8080/health

### Submit Document
POST http://localhost:8080/api/v1/gp-practice/GP12345/documents
Content-Type: application/fhir+json

{
  "resourceType": "Bundle",
  ...
}
```

## Customization

### Adding Extensions
Edit `.devcontainer/devcontainer.json` and add extension IDs to the `extensions` array.

### Changing Python Version
Edit the `image` property in `devcontainer.json`:
```json
"image": "mcr.microsoft.com/devcontainers/python:3.12-bullseye"
```

### Adding System Packages
Edit `.devcontainer/post-create.sh` to add `apt-get install` commands.

## Troubleshooting

### Container won't start
- Check Docker Desktop is running
- Try "Remote-Containers: Rebuild Container"

### Python packages not installing
- Check `pyproject.toml` is valid
- Try running `uv sync` manually in terminal

### Extensions not loading
- Check internet connection
- Try "Developer: Reload Window" from command palette

### ODBC driver issues
- Verify MS SQL Server driver installed: `odbcinst -j`
- Check driver configuration in `.env` file

## Additional Resources

- [VS Code Remote Containers](https://code.visualstudio.com/docs/remote/containers)
- [uv Documentation](https://github.com/astral-sh/uv)
- [just Documentation](https://github.com/casey/just)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
