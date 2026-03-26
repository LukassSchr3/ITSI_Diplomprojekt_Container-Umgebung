package itsi.api.database.service;

import itsi.api.database.dto.DashboardCourseDTO;
import itsi.api.database.entity.Course;
import itsi.api.database.entity.StudentCourse;
import itsi.api.database.entity.Task;
import itsi.api.database.repository.CourseRepository;
import itsi.api.database.repository.StudentCourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCourseServiceTest {

    @Mock
    private StudentCourseRepository studentCourseRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseTaskService courseTaskService;

    @InjectMocks
    private StudentCourseService studentCourseService;

    private StudentCourse testSC;
    private Course testCourse;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testSC = new StudentCourse();
        testSC.setUserId(1);
        testSC.setCourseId(10);
        testSC.setEnrolledAt(LocalDateTime.now());
        testSC.setExpiresAt(LocalDateTime.now().plusMonths(6));

        testCourse = new Course();
        testCourse.setId(10);
        testCourse.setName("Testkurs");
        testCourse.setDescription("Beschreibung");
        testCourse.setCreatedAt(LocalDateTime.now());

        testTask = new Task();
        testTask.setId(1);
        testTask.setTitle("Aufgabe 1");
        testTask.setDescription("Aufgabenbeschreibung");
        testTask.setPoints(10);
        testTask.setImageId(1);
        testTask.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void findAllShouldReturnAllStudentCourses() {
        when(studentCourseRepository.findAll()).thenReturn(Arrays.asList(testSC));

        List<StudentCourse> result = studentCourseService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(studentCourseRepository, times(1)).findAll();
    }

    @Test
    void findAllShouldReturnEmptyList() {
        when(studentCourseRepository.findAll()).thenReturn(Collections.emptyList());

        List<StudentCourse> result = studentCourseService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserIdShouldReturnCoursesForUser() {
        when(studentCourseRepository.findByUserId(1)).thenReturn(Arrays.asList(testSC));

        List<StudentCourse> result = studentCourseService.findByUserId(1);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getUserId());
        verify(studentCourseRepository).findByUserId(1);
    }

    @Test
    void findByCourseIdShouldReturnStudentsForCourse() {
        when(studentCourseRepository.findByCourseId(10)).thenReturn(Arrays.asList(testSC));

        List<StudentCourse> result = studentCourseService.findByCourseId(10);

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getCourseId());
        verify(studentCourseRepository).findByCourseId(10);
    }

    @Test
    void findByUserIdAndCourseIdShouldReturnStudentCourseWhenExists() {
        when(studentCourseRepository.findByUserIdAndCourseId(1, 10)).thenReturn(Optional.of(testSC));

        Optional<StudentCourse> result = studentCourseService.findByUserIdAndCourseId(1, 10);

        assertTrue(result.isPresent());
        assertEquals(testSC, result.get());
    }

    @Test
    void findByUserIdAndCourseIdShouldReturnEmptyWhenNotExists() {
        when(studentCourseRepository.findByUserIdAndCourseId(99, 99)).thenReturn(Optional.empty());

        Optional<StudentCourse> result = studentCourseService.findByUserIdAndCourseId(99, 99);

        assertFalse(result.isPresent());
    }

    @Test
    void saveShouldPersistAndReturnStudentCourse() {
        when(studentCourseRepository.save(testSC)).thenReturn(testSC);

        StudentCourse result = studentCourseService.save(testSC);

        assertNotNull(result);
        assertEquals(testSC, result);
        verify(studentCourseRepository).save(testSC);
    }

    @Test
    void deleteByUserIdAndCourseIdShouldCallDeleteById() {
        studentCourseService.deleteByUserIdAndCourseId(1, 10);

        verify(studentCourseRepository).deleteById(new StudentCourse.StudentCourseId(1, 10));
    }

    @Test
    void getDashboardCoursesByUserIdShouldReturnMappedDTOs() {
        when(studentCourseRepository.findByUserId(1)).thenReturn(Arrays.asList(testSC));
        when(courseRepository.findById(10)).thenReturn(Optional.of(testCourse));
        when(courseTaskService.findTasksByCourseId(10)).thenReturn(Arrays.asList(testTask));

        List<DashboardCourseDTO> result = studentCourseService.getDashboardCoursesByUserId(1);

        assertEquals(1, result.size());
        DashboardCourseDTO dto = result.get(0);
        assertEquals(10, dto.getCourseId());
        assertEquals("Testkurs", dto.getCourseName());
        assertEquals(1, dto.getTasks().size());
        assertEquals("Aufgabe 1", dto.getTasks().get(0).getTitle());
    }

    @Test
    void getDashboardCoursesByUserIdShouldSkipNullCourses() {
        when(studentCourseRepository.findByUserId(1)).thenReturn(Arrays.asList(testSC));
        when(courseRepository.findById(10)).thenReturn(Optional.empty());

        List<DashboardCourseDTO> result = studentCourseService.getDashboardCoursesByUserId(1);

        assertTrue(result.isEmpty());
    }

    @Test
    void getDashboardCoursesByUserIdShouldReturnEmptyListWhenNoEnrollments() {
        when(studentCourseRepository.findByUserId(99)).thenReturn(Collections.emptyList());

        List<DashboardCourseDTO> result = studentCourseService.getDashboardCoursesByUserId(99);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

