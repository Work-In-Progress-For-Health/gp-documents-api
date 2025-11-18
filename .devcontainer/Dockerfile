# Use the official .NET 8 SDK image
FROM mcr.microsoft.com/devcontainers/dotnet:8.0

# Install additional tools
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends \
    # Required for SQL Server tools
    curl \
    apt-transport-https \
    # Useful development tools
    vim \
    nano \
    git \
    jq \
    # For debugging and diagnostics
    procps \
    lsof \
    htop \
    net-tools \
    iputils-ping \
    dnsutils \
    && apt-get clean -y && rm -rf /var/lib/apt/lists/*

# Install SQL Server command-line tools (sqlcmd)
RUN curl https://packages.microsoft.com/keys/microsoft.asc | apt-key add - \
    && curl https://packages.microsoft.com/config/debian/11/prod.list > /etc/apt/sources.list.d/mssql-release.list \
    && apt-get update \
    && ACCEPT_EULA=Y apt-get install -y mssql-tools18 unixodbc-dev \
    && echo 'export PATH="$PATH:/opt/mssql-tools18/bin"' >> /home/vscode/.bashrc \
    && apt-get clean -y && rm -rf /var/lib/apt/lists/*

# Install Entity Framework Core tools globally
RUN dotnet tool install --global dotnet-ef \
    && dotnet tool install --global dotnet-aspnet-codegenerator

# Add dotnet tools to PATH
ENV PATH="${PATH}:/home/vscode/.dotnet/tools"

# Set the default shell to bash
SHELL ["/bin/bash", "-c"]

# Create a non-root user (already exists in the base image)
USER vscode

# Install user-level dotnet tools
RUN dotnet tool install --global dotnet-format \
    && dotnet tool install --global dotnet-outdated-tool \
    && dotnet tool install --global dotnet-reportgenerator-globaltool

WORKDIR /workspace
