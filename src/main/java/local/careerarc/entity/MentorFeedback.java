package local.careerarc.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mentor_feedback")
public class MentorFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Account mentor;
    @ManyToOne(optional = false) public Account student;
    @Column(nullable = false, length = 1800) public String message;
    @ManyToOne public CareerRole suggestedRole;
    @ManyToOne public LearningResource suggestedResource;
    @Column(nullable = false) public Instant createdAt = Instant.now();
    public MentorFeedback() {}
}