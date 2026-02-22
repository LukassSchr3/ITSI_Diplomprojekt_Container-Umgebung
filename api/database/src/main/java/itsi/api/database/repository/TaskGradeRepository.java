package itsi.api.database.repository;

import itsi.api.database.entity.TaskGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskGradeRepository extends JpaRepository<TaskGrade, Integer> {
    List<TaskGrade> findByUserId(Integer userId);
    List<TaskGrade> findByTaskId(Integer taskId);
    Optional<TaskGrade> findByUserIdAndTaskId(Integer userId, Integer taskId);
    List<TaskGrade> findByPassed(Boolean passed);
}
