package local.careerarc.repository;

import local.careerarc.entity.RoadmapMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoadmapMilestoneRepository extends JpaRepository<RoadmapMilestone, Long> {
    List<RoadmapMilestone> findByRoadmapIdOrderBySequenceNumberAsc(Long roadmapId);
    List<RoadmapMilestone> findByResourceId(Long resourceId);
}