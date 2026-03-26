package itsi.api.database.controller;

import itsi.api.database.entity.TaskGrade;
import itsi.api.database.service.TaskGradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskGradeControllerTest {

    @Mock
    private TaskGradeService taskGradeService;

    @InjectMocks
    private TaskGradeController taskGradeController;

    private TaskGrade testGrade;

    @BeforeEach
    void setUp() {
        testGrade = new TaskGrade();
        testGrade.setId(1);
        testGrade.setUserId(1);
        testGrade.setTaskId(10);
        testGrade.setGrade("Sehr Gut");
        testGrade.setPassed(true);
        testGrade.setFeedback("Gut gemacht");
        testGrade.setGradedAt(LocalDateTime.now());
    }

    @Test
    void getAllTaskGradesShouldReturnList() {
        when(taskGradeService.findAll()).thenReturn(Arrays.asList(testGrade));

        ResponseEntity<List<TaskGrade>> response = taskGradeController.getAllTaskGrades();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllTaskGradesShouldReturnEmptyList() {
        when(taskGradeService.findAll()).thenReturn(Collections.emptyList());

        assertTrue(taskGradeController.getAllTaskGrades().getBody().isEmpty());
    }

    @Test
    void getTaskGradeByIdShouldReturnWhenExists() {
        when(taskGradeService.findById(1)).thenReturn(Optional.of(testGrade));

        ResponseEntity<TaskGrade> response = taskGradeController.getTaskGradeById(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testGrade, response.getBody());
    }

    @Test
    void getTaskGradeByIdShouldReturn404WhenNotExists() {
        when(taskGradeService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, taskGradeController.getTaskGradeById(99).getStatusCode());
    }

    @Test
    void getGradesByUserIdShouldReturnList() {
        when(taskGradeService.findByUserId(1)).thenReturn(Arrays.asList(testGrade));

        ResponseEntity<List<TaskGrade>> response = taskGradeController.getGradesByUserId(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getGradesByTaskIdShouldReturnList() {
        when(taskGradeService.findByTaskId(10)).thenReturn(Arrays.asList(testGrade));

        ResponseEntity<List<TaskGrade>> response = taskGradeController.getGradesByTaskId(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getGradeByUserAndTaskShouldReturnWhenExists() {
        when(taskGradeService.findByUserIdAndTaskId(1, 10)).thenReturn(Optional.of(testGrade));

        ResponseEntity<TaskGrade> response = taskGradeController.getGradeByUserAndTask(1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testGrade, response.getBody());
    }

    @Test
    void getGradeByUserAndTaskShouldReturn404WhenNotExists() {
        when(taskGradeService.findByUserIdAndTaskId(99, 99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, taskGradeController.getGradeByUserAndTask(99, 99).getStatusCode());
    }

    @Test
    void getGradesByPassedShouldReturnPassedGrades() {
        when(taskGradeService.findByPassed(true)).thenReturn(Arrays.asList(testGrade));

        ResponseEntity<List<TaskGrade>> response = taskGradeController.getGradesByPassed(true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getGradesByPassedShouldReturnFailedGrades() {
        when(taskGradeService.findByPassed(false)).thenReturn(Collections.emptyList());

        ResponseEntity<List<TaskGrade>> response = taskGradeController.getGradesByPassed(false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void createTaskGradeShouldReturnCreated() {
        when(taskGradeService.findByUserIdAndTaskId(1, 10)).thenReturn(Optional.empty());
        when(taskGradeService.save(any(TaskGrade.class))).thenReturn(testGrade);

        ResponseEntity<?> response = taskGradeController.createTaskGrade(testGrade);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testGrade, response.getBody());
    }

    @Test
    void createTaskGradeShouldReturnConflictWhenAlreadyExists() {
        when(taskGradeService.findByUserIdAndTaskId(1, 10)).thenReturn(Optional.of(testGrade));

        ResponseEntity<?> response = taskGradeController.createTaskGrade(testGrade);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(taskGradeService, never()).save(any());
    }

    @Test
    void updateTaskGradeShouldReturnUpdated() {
        when(taskGradeService.findById(1)).thenReturn(Optional.of(testGrade));
        when(taskGradeService.save(any(TaskGrade.class))).thenReturn(testGrade);

        ResponseEntity<TaskGrade> response = taskGradeController.updateTaskGrade(1, testGrade);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testGrade, response.getBody());
    }

    @Test
    void updateTaskGradeShouldReturn404WhenNotExists() {
        when(taskGradeService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, taskGradeController.updateTaskGrade(99, testGrade).getStatusCode());
    }

    @Test
    void updateTaskGradeByUserAndTaskShouldReturnUpdated() {
        when(taskGradeService.findByUserIdAndTaskId(1, 10)).thenReturn(Optional.of(testGrade));
        when(taskGradeService.save(any(TaskGrade.class))).thenReturn(testGrade);

        ResponseEntity<TaskGrade> response = taskGradeController.updateTaskGradeByUserAndTask(1, 10, testGrade);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateTaskGradeByUserAndTaskShouldReturn404WhenNotExists() {
        when(taskGradeService.findByUserIdAndTaskId(99, 99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, taskGradeController.updateTaskGradeByUserAndTask(99, 99, testGrade).getStatusCode());
    }

    @Test
    void deleteTaskGradeShouldReturnNoContent() {
        when(taskGradeService.findById(1)).thenReturn(Optional.of(testGrade));

        ResponseEntity<Void> response = taskGradeController.deleteTaskGrade(1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(taskGradeService).deleteById(1);
    }

    @Test
    void deleteTaskGradeShouldReturn404WhenNotExists() {
        when(taskGradeService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, taskGradeController.deleteTaskGrade(99).getStatusCode());
        verify(taskGradeService, never()).deleteById(any());
    }
}

