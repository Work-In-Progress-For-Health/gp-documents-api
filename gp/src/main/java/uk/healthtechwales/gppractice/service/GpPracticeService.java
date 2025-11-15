package uk.healthtechwales.gppractice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.healthtechwales.gppractice.repository.GpPracticeRepository;

@Service
public class GpPracticeService {

    private final GpPracticeRepository gpPracticeRepository;

    @Autowired
    public GpPracticeService(GpPracticeRepository gpPracticeRepository) {
        this.gpPracticeRepository = gpPracticeRepository;
    }

    public boolean isValidPractice(String gpPracticeId) {
        return gpPracticeRepository.existsByGpPracticeId(gpPracticeId);
    }
}
