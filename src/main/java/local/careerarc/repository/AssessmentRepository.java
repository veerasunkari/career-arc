package local.careerarc.repository;

import local.careerarc.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByStudentIdOrderByCompletedAtDesc(Long studentId);
}