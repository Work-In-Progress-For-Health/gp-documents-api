import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .config import get_settings
from .controllers import router

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)

logger = logging.getLogger(__name__)

# Get settings
settings = get_settings()

# Update log level from settings
logging.getLogger().setLevel(settings.log_level)

# Create FastAPI app
app = FastAPI(
    title="GP Practice Document Submission API",
    description="API for submitting clinical documents to GP practices",
    version="4.1.0"
)

# Add CORS middleware (configure as needed)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(router)


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy"}


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": "GP Practice Document Submission API",
        "version": "4.1.0",
        "status": "running"
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "gp_practice_documents.main:app",
        host="0.0.0.0",
        port=settings.server_port,
        reload=False,
        log_level=settings.log_level.lower()
    )
