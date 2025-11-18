from sqlalchemy import select
from sqlalchemy.orm import Session

from ..entities import GpPractice


class GpPracticeRepository:
    """Repository for GP Practice operations."""

    def __init__(self, session: Session):
        self.session = session

    def exists_by_gp_practice_id(self, gp_practice_id: str) -> bool:
        """Check if a GP practice exists by ID."""
        stmt = select(GpPractice).where(GpPractice.gp_practice_id == gp_practice_id)
        result = self.session.execute(stmt).first()
        return result is not None
