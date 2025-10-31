package uk.nhs.wales.gppractice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "gp_practice")
public class GpPractice {

    @Id
    @Column(name = "gp_practice_id")
    private String gpPracticeId;

    @Column(name = "lhb_code")
    private String lhbCode;

    public String getGpPracticeId() {
        return gpPracticeId;
    }

    public void setGpPracticeId(String gpPracticeId) {
        this.gpPracticeId = gpPracticeId;
    }

    public String getLHBCode() {
        return lhbCode;
    }
}

