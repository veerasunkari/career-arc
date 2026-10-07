package local.careerarc.repository;

import local.careerarc.entity.CareerRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CareerRecommendationRepository extends JpaRepository<CareerRecommendation, Long> {
    List<CareerRecommendation> findByStudentIdOrderByScoreDescCalculatedAtDesc(Long studentId);
    void deleteByStudentId(Long studentId);
}