package local.careerarc.repository;

import local.careerarc.entity.MentorFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MentorFeedbackRepository extends JpaRepository<MentorFeedback, Long> {
    List<MentorFeedback> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}