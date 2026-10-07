package local.careerarc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_skill", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "skill_id"}))
public class StudentSkill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id") public Account student;
    @ManyToOne(optional = false) @JoinColumn(name = "skill_id") public Skill skill;
    @Column(nullable = false) public int proficiency;
    public StudentSkill() {}
    public StudentSkill(Account student, Skill skill, int proficiency) { this.student = student; this.skill = skill; this.proficiency = proficiency; }
}