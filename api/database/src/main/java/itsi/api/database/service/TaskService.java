package itsi.api.database.service;

import itsi.api.database.entity.Task;
import itsi.api.database.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    public Optional<Task> findById(Integer id) {
        return taskRepository.findById(id);
    }

    public List<Task> findByCourseId(Integer courseId) {
        return taskRepository.findByCourseId(courseId);
    }

    public List<Task> findByImageId(Integer imageId) {
        return taskRepository.findByImageId(imageId);
    }

    public Task save(Task task) {
        return taskRepository.save(task);
    }

    public void deleteById(Integer id) {
        taskRepository.deleteById(id);
    }
}
