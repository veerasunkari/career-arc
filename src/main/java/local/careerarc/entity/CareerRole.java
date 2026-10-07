package local.careerarc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "career_role", uniqueConstraints = @UniqueConstraint(columnNames = "title"))
public class CareerRole {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 120) public String title;
    @Column(nullable = false, length = 100) public String domain;
    @Column(length = 1200) public String description;
    @Column(length = 300) public String academicFit;
    @Column(length = 400) public String interests;
    public boolean active = true;
    public CareerRole() {}
    public CareerRole(String title, String domain, String description, String academicFit, String interests) {
        this.title = title; this.domain = domain; this.description = description; this.academicFit = academicFit; this.interests = interests;
    }
}