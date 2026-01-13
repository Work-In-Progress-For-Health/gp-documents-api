from enum import Enum


class ProcessingMode(str, Enum):
    """Document processing modes for malware scanning."""

    SYNC = "SYNC"
    ASYNC = "ASYNC"
    HYBRID = "HYBRID"
