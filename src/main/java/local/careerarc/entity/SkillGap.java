package local.careerarc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "skill_gap", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "role_id", "skill_id"}))
public class SkillGap {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id") public Account student;
    @ManyToOne(optional = false) @JoinColumn(name = "role_id") public CareerRole role;
    @ManyToOne(optional = false) @JoinColumn(name = "skill_id") public Skill skill;
    public int currentLevel;
    public int requiredLevel;
    @Column(length = 24) public String status;
    public int priority;
    public SkillGap() {}
}