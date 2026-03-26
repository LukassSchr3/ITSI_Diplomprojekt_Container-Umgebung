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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void findAllShouldReturnAllCourseTasks() {
        when(courseTaskRepository.findAll()).thenReturn(Arrays.asList(testCourseTask));

        List<CourseTask> result = courseTaskService.findAll();

        assertEquals(1, result.size());
        verify(courseTaskRepository).findAll();
    }

    @Test
    void findAllShouldReturnEmptyList() {
        when(courseTaskRepository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(courseTaskService.findAll().isEmpty());
    }

    @Test
    void findByCourseIdShouldReturnCourseTasks() {
        when(courseTaskRepository.findByCourseIdOrderByOrderIndex(10)).thenReturn(Arrays.asList(testCourseTask));

        List<CourseTask> result = courseTaskService.findByCourseId(10);

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getCourseId());
        verify(courseTaskRepository).findByCourseIdOrderByOrderIndex(10);
    }

    @Test
    void findTasksByCourseIdShouldReturnTaskObjects() {
        when(courseTaskRepository.findByCourseIdOrderByOrderIndex(10)).thenReturn(Arrays.asList(testCourseTask));
        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));

        List<Task> result = courseTaskService.findTasksByCourseId(10);

        assertEquals(1, result.size());
        assertEquals("Aufgabe 1", result.get(0).getTitle());
    }

    @Test
    void findTasksByCourseIdShouldSkipMissingTasks() {
        when(courseTaskRepository.findByCourseIdOrderByOrderIndex(10)).thenReturn(Arrays.asList(testCourseTask));
        when(taskRepository.findById(1)).thenReturn(Optional.empty());

        List<Task> result = courseTaskService.findTasksByCourseId(10);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByTaskIdShouldReturnCourseTasks() {
        when(courseTaskRepository.findByTaskId(1)).thenReturn(Arrays.asList(testCourseTask));

        List<CourseTask> result = courseTaskService.findByTaskId(1);

        assertEquals(1, result.size());
        verify(courseTaskRepository).findByTaskId(1);
    }

    @Test
    void findByCourseIdAndTaskIdShouldReturnWhenExists() {
        when(courseTaskRepository.findByCourseIdAndTaskId(10, 1)).thenReturn(Optional.of(testCourseTask));

        Optional<CourseTask> result = courseTaskService.findByCourseIdAndTaskId(10, 1);

        assertTrue(result.isPresent());
        assertEquals(testCourseTask, result.get());
    }

    @Test
    void findByCourseIdAndTaskIdShouldReturnEmptyWhenNotExists() {
        when(courseTaskRepository.findByCourseIdAndTaskId(99, 99)).thenReturn(Optional.empty());

        assertFalse(courseTaskService.findByCourseIdAndTaskId(99, 99).isPresent());
    }

    @Test
    void saveShouldPersistAndReturnCourseTask() {
        when(courseTaskRepository.save(testCourseTask)).thenReturn(testCourseTask);

        CourseTask result = courseTaskService.save(testCourseTask);

        assertNotNull(result);
        assertEquals(testCourseTask, result);
        verify(courseTaskRepository).save(testCourseTask);
    }

    @Test
    void deleteByCourseIdAndTaskIdShouldCallDeleteById() {
        courseTaskService.deleteByCourseIdAndTaskId(10, 1);

        verify(courseTaskRepository).deleteById(new CourseTask.CourseTaskId(10, 1));
    }
}

