package local.careerarc.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "roadmap")
public class Roadmap {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Account student;
    @ManyToOne(optional = false) public CareerRole role;
    @Column(nullable = false) public Instant generatedAt = Instant.now();
    public boolean active = true;
    public Roadmap() {}
}