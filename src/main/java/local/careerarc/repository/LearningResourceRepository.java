package local.careerarc.repository;

import local.careerarc.entity.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {
    List<LearningResource> findAllByOrderByTitleAsc();
    List<LearningResource> findBySkillId(Long skillId);
    boolean existsByTitleIgnoreCase(String title);
}