package itsi.api.database.repository;

import itsi.api.database.entity.LiveEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LiveEnvironmentRepository extends JpaRepository<LiveEnvironment, Long> {
    @Query("SELECT MAX(le.vncPort) FROM LiveEnvironment le")
    Integer findMaxVncPort();

    // Neuer Finder nach userId
    Optional<LiveEnvironment> findByUserId(Long userId);

    // Löschen nach userId (abgeleitete Query)
    void deleteByUserId(Long userId);
}
