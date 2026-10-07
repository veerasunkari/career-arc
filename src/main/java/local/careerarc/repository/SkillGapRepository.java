package local.careerarc.repository;

import local.careerarc.entity.SkillGap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillGapRepository extends JpaRepository<SkillGap, Long> {
    List<SkillGap> findByStudentIdAndRoleIdOrderByPriorityAsc(Long studentId, Long roleId);
    void deleteByStudentIdAndRoleId(Long studentId, Long roleId);
}