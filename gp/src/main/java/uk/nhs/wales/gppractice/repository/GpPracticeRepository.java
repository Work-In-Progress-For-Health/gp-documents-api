package uk.nhs.wales.gppractice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.nhs.wales.gppractice.entity.GpPractice;

public interface GpPracticeRepository extends JpaRepository<GpPractice, String> {
    boolean existsByGpPracticeId(String gpPracticeId);
}
