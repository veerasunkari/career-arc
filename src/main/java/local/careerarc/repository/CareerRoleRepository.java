package local.careerarc.repository;

import local.careerarc.entity.CareerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CareerRoleRepository extends JpaRepository<CareerRole, Long> {
    List<CareerRole> findByActiveTrueOrderByTitleAsc();
    Optional<CareerRole> findByTitleIgnoreCase(String title);
}