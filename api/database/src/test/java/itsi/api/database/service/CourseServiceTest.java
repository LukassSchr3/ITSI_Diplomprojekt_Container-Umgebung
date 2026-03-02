package itsi.api.database.service;

import itsi.api.database.entity.Course;
import itsi.api.database.repository.CourseRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setId(1);
        testCourse.setName("5AHIT");
        testCourse.setDescription("Beschreibung");
        testCourse.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void findAll_shouldReturnAllCourses() {
        when(courseRepository.findAll()).thenReturn(Arrays.asList(testCourse));

        List<Course> result = courseService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyList() {
        when(courseRepository.findAll()).thenReturn(Collections.emptyList());

        List<Course> result = courseService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findById_shouldReturnCourseWhenExists() {
        when(courseRepository.findById(1)).thenReturn(Optional.of(testCourse));

        Optional<Course> result = courseService.findById(1);

        assertTrue(result.isPresent());
        assertEquals(testCourse, result.get());
        verify(courseRepository).findById(1);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Course> result = courseService.findById(99);

        assertFalse(result.isPresent());
    }

    @Test
    void findByName_shouldReturnCourseWhenExists() {
        when(courseRepository.findByName("5AHIT")).thenReturn(Optional.of(testCourse));

        Optional<Course> result = courseService.findByName("5AHIT");

        assertTrue(result.isPresent());
        assertEquals("5AHIT", result.get().getName());
        verify(courseRepository).findByName("5AHIT");
    }

    @Test
    void findByName_shouldReturnEmptyWhenNotExists() {
        when(courseRepository.findByName("NONE")).thenReturn(Optional.empty());

        Optional<Course> result = courseService.findByName("NONE");

        assertFalse(result.isPresent());
    }

    @Test
    void save_shouldPersistAndReturnCourse() {
        when(courseRepository.save(testCourse)).thenReturn(testCourse);

        Course result = courseService.save(testCourse);

        assertNotNull(result);
        assertEquals(testCourse, result);
        verify(courseRepository).save(testCourse);
    }

    @Test
    void deleteById_shouldCallRepository() {
        courseService.deleteById(1);

        verify(courseRepository).deleteById(1);
    }
}

