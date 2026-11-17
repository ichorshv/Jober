package org.jober.jober.interview;

import jakarta.persistence.*;
import org.jober.jober.application.Application;
import org.jober.jober.common.enums.InterviewOutcome;
import org.jober.jober.common.enums.InterviewType;

import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewType type;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "location_or_link")
    private String locationOrLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewOutcome outcome = InterviewOutcome.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public Interview() {}


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Application getApplication() { return application; }
    public void setApplication(Application application) { this.application = application; }

    public InterviewType getType() { return type; }
    public void setType(InterviewType type) { this.type = type; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getLocationOrLink() { return locationOrLink; }
    public void setLocationOrLink(String locationOrLink) { this.locationOrLink = locationOrLink; }

    public InterviewOutcome getOutcome() { return outcome; }
    public void setOutcome(InterviewOutcome outcome) { this.outcome = outcome; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
