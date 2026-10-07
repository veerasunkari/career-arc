package local.careerarc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_profile")
public class StudentProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @OneToOne(optional = false) @JoinColumn(unique = true) public Account student;
    @Column(length = 50) public String educationLevel;
    @Column(length = 120) public String fieldOfStudy;
    public Double academicScore;
    @Column(length = 500) public String interests;
    @Column(length = 500) public String preferredDomains;
    @Column(length = 1000) public String careerGoals;
    @Column(length = 30) public String phone;
    @ManyToOne public CareerRole targetCareerRole;
    public StudentProfile() {}
    public StudentProfile(Account student) { this.student = student; }
}