package local.careerarc.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "assessment")
public class Assessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Account student;
    public int analyticalScore;
    public int creativeScore;
    public int communicationScore;
    public int practicalScore;
    @Column(length = 500) public String interests;
    @Column(length = 500) public String preferredDomains;
    @Column(nullable = false) public Instant completedAt = Instant.now();
    public Assessment() {}
}