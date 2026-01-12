package itsi.api.database.repository;

import itsi.api.database.entity.LiveEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LiveEnvironmentRepository extends JpaRepository<LiveEnvironment, Long> {
    @Query("SELECT MAX(le.vncPort) FROM LiveEnvironment le")
    Integer findMaxVncPort();
}
