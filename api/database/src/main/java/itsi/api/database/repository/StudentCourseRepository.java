package itsi.api.database.repository;

import itsi.api.database.entity.StudentCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourse, StudentCourse.StudentCourseId> {
    List<StudentCourse> findByUserId(Integer userId);
    List<StudentCourse> findByCourseId(Integer courseId);
    Optional<StudentCourse> findByUserIdAndCourseId(Integer userId, Integer courseId);
}
