package local.careerarc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "mentor_assignment", uniqueConstraints = @UniqueConstraint(columnNames = {"mentor_id", "student_id"}))
public class MentorAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "mentor_id") public Account mentor;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id") public Account student;
    public MentorAssignment() {}
    public MentorAssignment(Account mentor, Account student) { this.mentor = mentor; this.student = student; }
}