package local.careerarc.repository;

import local.careerarc.entity.MentorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MentorAssignmentRepository extends JpaRepository<MentorAssignment, Long> {
    List<MentorAssignment> findByMentorId(Long mentorId);
    Optional<MentorAssignment> findByMentorIdAndStudentId(Long mentorId, Long studentId);
}