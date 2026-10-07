package local.careerarc.repository;

import local.careerarc.entity.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {
    Optional<Roadmap> findFirstByStudentIdAndActiveTrueOrderByGeneratedAtDesc(Long studentId);
}