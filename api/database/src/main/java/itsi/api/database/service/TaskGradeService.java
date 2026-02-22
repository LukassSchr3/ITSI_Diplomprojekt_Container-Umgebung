package itsi.api.database.service;

import itsi.api.database.entity.TaskGrade;
import itsi.api.database.repository.TaskGradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskGradeService {

    private final TaskGradeRepository taskGradeRepository;

    public List<TaskGrade> findAll() {
        return taskGradeRepository.findAll();
    }

    public Optional<TaskGrade> findById(Integer id) {
        return taskGradeRepository.findById(id);
    }

    public List<TaskGrade> findByUserId(Integer userId) {
        return taskGradeRepository.findByUserId(userId);
    }

    public List<TaskGrade> findByTaskId(Integer taskId) {
        return taskGradeRepository.findByTaskId(taskId);
    }

    public Optional<TaskGrade> findByUserIdAndTaskId(Integer userId, Integer taskId) {
        return taskGradeRepository.findByUserIdAndTaskId(userId, taskId);
    }

    public List<TaskGrade> findByPassed(Boolean passed) {
        return taskGradeRepository.findByPassed(passed);
    }

    public TaskGrade save(TaskGrade taskGrade) {
        return taskGradeRepository.save(taskGrade);
    }

    public void deleteById(Integer id) {
        taskGradeRepository.deleteById(id);
    }
}
