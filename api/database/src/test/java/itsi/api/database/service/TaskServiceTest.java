package itsi.api.database.service;

import itsi.api.database.entity.Task;
import itsi.api.database.repository.TaskRepository;
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
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;

    @BeforeEach
    void setUp() {
        testTask = new Task();
        testTask.setId(1);
        testTask.setTitle("Aufgabe 1");
        testTask.setDescription("Beschreibung");
        testTask.setPoints(10);
        testTask.setImageId(5);
        testTask.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void findAll_shouldReturnAllTasks() {
        when(taskRepository.findAll()).thenReturn(Arrays.asList(testTask));

        List<Task> result = taskService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyList() {
        when(taskRepository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(taskService.findAll().isEmpty());
    }

    @Test
    void findById_shouldReturnTaskWhenExists() {
        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));

        Optional<Task> result = taskService.findById(1);

        assertTrue(result.isPresent());
        assertEquals(testTask, result.get());
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        when(taskRepository.findById(99)).thenReturn(Optional.empty());

        assertFalse(taskService.findById(99).isPresent());
    }

    @Test
    void findByImageId_shouldReturnTasks() {
        when(taskRepository.findByImageId(5)).thenReturn(Arrays.asList(testTask));

        List<Task> result = taskService.findByImageId(5);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getImageId());
        verify(taskRepository).findByImageId(5);
    }

    @Test
    void findByImageId_shouldReturnEmptyList() {
        when(taskRepository.findByImageId(99)).thenReturn(Collections.emptyList());

        assertTrue(taskService.findByImageId(99).isEmpty());
    }

    @Test
    void save_shouldPersistAndReturnTask() {
        when(taskRepository.save(testTask)).thenReturn(testTask);

        Task result = taskService.save(testTask);

        assertNotNull(result);
        assertEquals(testTask, result);
        verify(taskRepository).save(testTask);
    }

    @Test
    void deleteById_shouldCallRepository() {
        taskService.deleteById(1);

        verify(taskRepository).deleteById(1);
    }
}

