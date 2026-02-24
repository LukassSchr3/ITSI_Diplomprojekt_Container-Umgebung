package itsi.api.database.service;

import itsi.api.database.dto.DashboardCourseDTO;
import itsi.api.database.entity.Course;
import itsi.api.database.entity.StudentCourse;
import itsi.api.database.entity.Task;
import itsi.api.database.repository.CourseRepository;
import itsi.api.database.repository.StudentCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentCourseService {

    private final StudentCourseRepository studentCourseRepository;
    private final CourseRepository courseRepository;
    private final CourseTaskService courseTaskService;

    public List<StudentCourse> findAll() {
        return studentCourseRepository.findAll();
    }

    public List<StudentCourse> findByUserId(Integer userId) {
        return studentCourseRepository.findByUserId(userId);
    }

    public List<StudentCourse> findByCourseId(Integer courseId) {
        return studentCourseRepository.findByCourseId(courseId);
    }

    public Optional<StudentCourse> findByUserIdAndCourseId(Integer userId, Integer courseId) {
        return studentCourseRepository.findByUserIdAndCourseId(userId, courseId);
    }

    public StudentCourse save(StudentCourse studentCourse) {
        return studentCourseRepository.save(studentCourse);
    }

    public void deleteByUserIdAndCourseId(Integer userId, Integer courseId) {
        StudentCourse.StudentCourseId id = new StudentCourse.StudentCourseId(userId, courseId);
        studentCourseRepository.deleteById(id);
    }

    public List<DashboardCourseDTO> getDashboardCoursesByUserId(Integer userId) {
        List<StudentCourse> studentCourses = studentCourseRepository.findByUserId(userId);
        
        return studentCourses.stream()
                .map(sc -> {
                    Course course = courseRepository.findById(sc.getCourseId()).orElse(null);
                    if (course == null) {
                        return null;
                    }
                    
                    List<Task> tasks = courseTaskService.findTasksByCourseId(sc.getCourseId());
                    List<DashboardCourseDTO.TaskDTO> taskDTOs = tasks.stream()
                            .map(task -> new DashboardCourseDTO.TaskDTO(
                                    task.getId(),
                                    task.getTitle(),
                                    task.getDescription(),
                                    task.getPoints(),
                                    task.getImageId()
                            ))
                            .collect(Collectors.toList());
                    
                    return new DashboardCourseDTO(
                            course.getId(),
                            course.getName(),
                            course.getDescription(),
                            sc.getEnrolledAt(),
                            sc.getExpiresAt(),
                            taskDTOs
                    );
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
}
