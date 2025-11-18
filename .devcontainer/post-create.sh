#!/bin/bash

set -e

echo "🚀 Setting up development environment..."

# Install uv
echo "📦 Installing uv..."
curl -LsSf https://astral.sh/uv/install.sh | sh
export PATH="/home/vscode/.local/bin:$PATH"

# Install just
echo "📦 Installing just..."
curl --proto '=https' --tlsv1.2 -sSf https://just.systems/install.sh | bash -s -- --to /usr/local/bin 2>/dev/null || \
wget https://github.com/casey/just/releases/download/1.36.0/just-1.36.0-x86_64-unknown-linux-musl.tar.gz -O /tmp/just.tar.gz && \
tar -xzf /tmp/just.tar.gz -C /tmp && \
sudo mv /tmp/just /usr/local/bin/just && \
sudo chmod +x /usr/local/bin/just && \
rm /tmp/just.tar.gz

# Install MS SQL Server ODBC Driver (needed for pyodbc)
echo "📦 Installing MS SQL Server ODBC Driver..."
curl https://packages.microsoft.com/keys/microsoft.asc | sudo apt-key add - 2>/dev/null || true
curl https://packages.microsoft.com/config/debian/11/prod.list | sudo tee /etc/apt/sources.list.d/mssql-release.list
sudo apt-get update
sudo ACCEPT_EULA=Y apt-get install -y msodbcsql18 unixodbc-dev

# Install Python dependencies
echo "📦 Installing Python dependencies..."
uv sync

# Copy environment file if it doesn't exist
if [ ! -f .env ]; then
    echo "📝 Creating .env file from template..."
    cp .env.example .env
fi

# Create necessary directories
mkdir -p tests

echo "✅ Development environment setup complete!"
echo ""
echo "🎯 Quick Start:"
echo "  - Run 'just --list' to see available commands"
echo "  - Run 'just dev' to start the development server"
echo "  - Visit http://localhost:8080/docs for API documentation"
echo ""
echo "📚 Useful commands:"
echo "  just install       - Install dependencies"
echo "  just dev           - Run with auto-reload"
echo "  just test          - Run tests"
echo "  just lint          - Lint code"
echo "  just format        - Format code"
echo ""
