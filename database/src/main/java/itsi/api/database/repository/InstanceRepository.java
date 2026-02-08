package itsi.api.database.repository;

import itsi.api.database.entity.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, Integer> {
    Optional<Instance> findByContainerId(String containerId);
    Optional<Instance> findByName(String name);
    List<Instance> findByUserId(Integer userId);
    List<Instance> findByImageId(Integer imageId);
    List<Instance> findByStatus(String status);

    // Größte Container-ID (lexikographisch) ermitteln
    Optional<Instance> findTopByOrderByContainerIdDesc();
}
