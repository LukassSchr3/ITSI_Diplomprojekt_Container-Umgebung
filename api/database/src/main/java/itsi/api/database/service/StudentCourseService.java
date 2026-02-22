package itsi.api.database.service;

import itsi.api.database.entity.StudentCourse;
import itsi.api.database.repository.StudentCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentCourseService {

    private final StudentCourseRepository studentCourseRepository;

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
}
