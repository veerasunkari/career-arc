package local.careerarc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "skill", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Skill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 100) public String name;
    @Column(nullable = false, length = 20) public String category;
    @ManyToOne @JoinColumn(name = "prerequisite_id") public Skill prerequisite;
    public Skill() {}
    public Skill(String name, String category) { this.name = name; this.category = category; }
}