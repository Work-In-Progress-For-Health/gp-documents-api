from sqlalchemy import Column, String

from .base import Base


class GpPractice(Base):
    """GP Practice entity."""

    __tablename__ = "gp_practice"

    gp_practice_id = Column(String, primary_key=True, name="gp_practice_id")
    lhb_code = Column(String, name="lhb_code")
