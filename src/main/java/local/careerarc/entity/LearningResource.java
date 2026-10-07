package local.careerarc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "learning_resource")
public class LearningResource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 180) public String title;
    @Column(length = 1200) public String description;
    @Column(nullable = false, length = 30) public String type;
    @ManyToOne @JoinColumn(name = "skill_id") public Skill skill;
    @ManyToOne @JoinColumn(name = "role_id") public CareerRole role;
    @Column(length = 24) public String difficulty;
    @Column(length = 500) public String url;
    public boolean illustrative = true;
    public LearningResource() {}
}