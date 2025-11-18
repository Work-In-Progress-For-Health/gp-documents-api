# GP Practice Documents API - Justfile
# Command runner for common development tasks

# Default recipe to display help information
default:
    @just --list

# Install dependencies
install:
    uv sync

# Install development dependencies
install-dev:
    uv sync --extra dev

# Run the application
run:
    uv run python -m uvicorn gp_practice_documents.main:app --host 0.0.0.0 --port 8080

# Run the application with auto-reload for development
dev:
    uv run python -m uvicorn gp_practice_documents.main:app --host 0.0.0.0 --port 8080 --reload

# Run tests
test:
    uv run pytest

# Run tests with coverage
test-coverage:
    uv run pytest --cov=gp_practice_documents --cov-report=html --cov-report=term

# Format code with ruff
format:
    uv run ruff format src/ tests/

# Lint code with ruff
lint:
    uv run ruff check src/ tests/

# Lint and fix code issues
lint-fix:
    uv run ruff check --fix src/ tests/

# Type check with mypy (if added)
typecheck:
    uv run mypy src/

# Run all code quality checks
check: lint test

# Clean build artifacts
clean:
    rm -rf .pytest_cache
    rm -rf .ruff_cache
    rm -rf htmlcov
    rm -rf .coverage
    rm -rf dist
    rm -rf build
    rm -rf *.egg-info
    find . -type d -name __pycache__ -exec rm -rf {} +
    find . -type f -name "*.pyc" -delete

# Build the project
build:
    uv build

# Run database migrations (placeholder)
migrate:
    @echo "Database migrations not yet implemented"

# Docker commands
docker-build:
    docker build -t gp-practice-documents:4.1.0 .

docker-run:
    docker run -p 8080:8080 --env-file .env gp-practice-documents:4.1.0

# Show project info
info:
    @echo "GP Practice Documents API"
    @echo "Version: 4.1.0"
    @echo "Python: $(python --version)"
    @uv --version
