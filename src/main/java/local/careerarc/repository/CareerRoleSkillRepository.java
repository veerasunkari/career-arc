package local.careerarc.repository;

import local.careerarc.entity.CareerRoleSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CareerRoleSkillRepository extends JpaRepository<CareerRoleSkill, Long> {
    List<CareerRoleSkill> findByRoleId(Long roleId);
    java.util.Optional<CareerRoleSkill> findByRoleIdAndSkillId(Long roleId, Long skillId);
}