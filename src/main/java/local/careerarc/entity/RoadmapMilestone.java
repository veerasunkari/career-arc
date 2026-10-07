package local.careerarc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roadmap_milestone")
public class RoadmapMilestone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Roadmap roadmap;
    @ManyToOne public Skill skill;
    @ManyToOne public LearningResource resource;
    @Column(nullable = false, length = 180) public String title;
    @Column(length = 900) public String description;
    @Column(nullable = false) public int sequenceNumber;
    @Column(nullable = false, length = 24) public String status = "NOT_STARTED";
    public RoadmapMilestone() {}
}