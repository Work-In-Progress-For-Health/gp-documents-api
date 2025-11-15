package uk.healthtechwales.gppractice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.healthtechwales.gppractice.entity.GpPractice;

public interface GpPracticeRepository extends JpaRepository<GpPractice, String> {
    boolean existsByGpPracticeId(String gpPracticeId);
}
