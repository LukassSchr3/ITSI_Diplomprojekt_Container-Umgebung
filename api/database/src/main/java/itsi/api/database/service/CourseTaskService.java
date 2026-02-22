package itsi.api.database.service;

import itsi.api.database.entity.CourseTask;
import itsi.api.database.entity.Task;
import itsi.api.database.repository.CourseTaskRepository;
import itsi.api.database.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseTaskService {

    private final CourseTaskRepository courseTaskRepository;
    private final TaskRepository taskRepository;

    public List<CourseTask> findAll() {
        return courseTaskRepository.findAll();
    }

    public List<CourseTask> findByCourseId(Integer courseId) {
        return courseTaskRepository.findByCourseIdOrderByOrderIndex(courseId);
    }

    public List<Task> findTasksByCourseId(Integer courseId) {
        List<CourseTask> courseTasks = courseTaskRepository.findByCourseIdOrderByOrderIndex(courseId);
        return courseTasks.stream()
                .map(ct -> taskRepository.findById(ct.getTaskId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public List<CourseTask> findByTaskId(Integer taskId) {
        return courseTaskRepository.findByTaskId(taskId);
    }

    public Optional<CourseTask> findByCourseIdAndTaskId(Integer courseId, Integer taskId) {
        return courseTaskRepository.findByCourseIdAndTaskId(courseId, taskId);
    }

    public CourseTask save(CourseTask courseTask) {
        return courseTaskRepository.save(courseTask);
    }

    public void deleteByCourseIdAndTaskId(Integer courseId, Integer taskId) {
        CourseTask.CourseTaskId id = new CourseTask.CourseTaskId(courseId, taskId);
        courseTaskRepository.deleteById(id);
    }
}
