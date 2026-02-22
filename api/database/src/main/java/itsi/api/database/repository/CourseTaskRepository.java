package itsi.api.database.repository;

import itsi.api.database.entity.CourseTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseTaskRepository extends JpaRepository<CourseTask, CourseTask.CourseTaskId> {
    List<CourseTask> findByCourseIdOrderByOrderIndex(Integer courseId);
    List<CourseTask> findByTaskId(Integer taskId);
    Optional<CourseTask> findByCourseIdAndTaskId(Integer courseId, Integer taskId);
}
