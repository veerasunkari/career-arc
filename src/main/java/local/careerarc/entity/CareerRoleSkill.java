package local.careerarc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "career_role_skill", uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "skill_id"}))
public class CareerRoleSkill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "role_id") public CareerRole role;
    @ManyToOne(optional = false) @JoinColumn(name = "skill_id") public Skill skill;
    @Column(nullable = false) public int requiredLevel;
    public CareerRoleSkill() {}
    public CareerRoleSkill(CareerRole role, Skill skill, int requiredLevel) { this.role = role; this.skill = skill; this.requiredLevel = requiredLevel; }
}