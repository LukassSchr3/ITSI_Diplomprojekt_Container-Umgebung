package itsi.api.database.service;

import itsi.api.database.entity.CourseTask;
import itsi.api.database.entity.Task;
import itsi.api.database.repository.CourseTaskRepository;
import itsi.api.database.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseTaskServiceTest {

    @Mock
    private CourseTaskRepository courseTaskRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private CourseTaskService courseTaskService;

    private CourseTask testCourseTask;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testCourseTask = new CourseTask();
        testCourseTask.setCourseId(10);
        testCourseTask.setTaskId(1);
        testCourseTask.setOrderIndex(0);

        testTask = new Task();
        testTask.setId(1);
        testTask.setTitle("Aufgabe 1");
        testTask.setPoints(10);
        testTask.setImageId(5);
    }

    @Test
    void findAll_shouldReturnAllCourseTasks() {
        when(courseTaskRepository.findAll()).thenReturn(Arrays.asList(testCourseTask));

        List<CourseTask> result = courseTaskService.findAll();

        assertEquals(1, result.size());
        verify(courseTaskRepository).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyList() {
        when(courseTaskRepository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(courseTaskService.findAll().isEmpty());
    }

    @Test
    void findByCourseId_shouldReturnCourseTasks() {
        when(courseTaskRepository.findByCourseIdOrderByOrderIndex(10)).thenReturn(Arrays.asList(testCourseTask));

        List<CourseTask> result = courseTaskService.findByCourseId(10);

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getCourseId());
        verify(courseTaskRepository).findByCourseIdOrderByOrderIndex(10);
    }

    @Test
    void findTasksByCourseId_shouldReturnTaskObjects() {
        when(courseTaskRepository.findByCourseIdOrderByOrderIndex(10)).thenReturn(Arrays.asList(testCourseTask));
        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));

        List<Task> result = courseTaskService.findTasksByCourseId(10);

        assertEquals(1, result.size());
        assertEquals("Aufgabe 1", result.get(0).getTitle());
    }

    @Test
    void findTasksByCourseId_shouldSkipMissingTasks() {
        when(courseTaskRepository.findByCourseIdOrderByOrderIndex(10)).thenReturn(Arrays.asList(testCourseTask));
        when(taskRepository.findById(1)).thenReturn(Optional.empty());

        List<Task> result = courseTaskService.findTasksByCourseId(10);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByTaskId_shouldReturnCourseTasks() {
        when(courseTaskRepository.findByTaskId(1)).thenReturn(Arrays.asList(testCourseTask));

        List<CourseTask> result = courseTaskService.findByTaskId(1);

        assertEquals(1, result.size());
        verify(courseTaskRepository).findByTaskId(1);
    }

    @Test
    void findByCourseIdAndTaskId_shouldReturnWhenExists() {
        when(courseTaskRepository.findByCourseIdAndTaskId(10, 1)).thenReturn(Optional.of(testCourseTask));

        Optional<CourseTask> result = courseTaskService.findByCourseIdAndTaskId(10, 1);

        assertTrue(result.isPresent());
        assertEquals(testCourseTask, result.get());
    }

    @Test
    void findByCourseIdAndTaskId_shouldReturnEmptyWhenNotExists() {
        when(courseTaskRepository.findByCourseIdAndTaskId(99, 99)).thenReturn(Optional.empty());

        assertFalse(courseTaskService.findByCourseIdAndTaskId(99, 99).isPresent());
    }

    @Test
    void save_shouldPersistAndReturnCourseTask() {
        when(courseTaskRepository.save(testCourseTask)).thenReturn(testCourseTask);

        CourseTask result = courseTaskService.save(testCourseTask);

        assertNotNull(result);
        assertEquals(testCourseTask, result);
        verify(courseTaskRepository).save(testCourseTask);
    }

    @Test
    void deleteByCourseIdAndTaskId_shouldCallDeleteById() {
        courseTaskService.deleteByCourseIdAndTaskId(10, 1);

        verify(courseTaskRepository).deleteById(new CourseTask.CourseTaskId(10, 1));
    }
}

