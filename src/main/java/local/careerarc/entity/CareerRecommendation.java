package local.careerarc.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "career_recommendation")
public class CareerRecommendation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Account student;
    @ManyToOne(optional = false) public CareerRole role;
    public int score;
    @Column(length = 1000) public String matchingSkills;
    @Column(length = 1000) public String relevantFactors;
    @Column(length = 1000) public String missingSkills;
    @Column(length = 1400) public String explanation;
    @Column(nullable = false) public Instant calculatedAt = Instant.now();
    public CareerRecommendation() {}
}