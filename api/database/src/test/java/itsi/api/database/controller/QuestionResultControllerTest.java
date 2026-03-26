package itsi.api.database.controller;

import itsi.api.database.entity.QuestionResult;
import itsi.api.database.service.QuestionResultService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionResultControllerTest {

    @Mock
    private QuestionResultService questionResultService;

    @InjectMocks
    private QuestionResultController questionResultController;

    private QuestionResult testResult;

    @BeforeEach
    void setUp() {
        testResult = new QuestionResult();
        testResult.setId(1);
        testResult.setUserId(1);
        testResult.setQuestionId(10);
        testResult.setErreichtePunkte(8);
        testResult.setBestanden(true);
        testResult.setCompletedAt(LocalDateTime.now());
    }

    @Test
    void getAllResults_shouldReturnList() {
        when(questionResultService.findAll()).thenReturn(Arrays.asList(testResult));

        ResponseEntity<List<QuestionResult>> response = questionResultController.getAllResults();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllResults_shouldReturnEmptyList() {
        when(questionResultService.findAll()).thenReturn(Collections.emptyList());

        assertTrue(questionResultController.getAllResults().getBody().isEmpty());
    }

    @Test
    void getResultById_shouldReturnWhenExists() {
        when(questionResultService.findById(1)).thenReturn(Optional.of(testResult));

        ResponseEntity<QuestionResult> response = questionResultController.getResultById(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testResult, response.getBody());
    }

    @Test
    void getResultById_shouldReturn404WhenNotExists() {
        when(questionResultService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, questionResultController.getResultById(99).getStatusCode());
    }

    @Test
    void getResultsByUserId_shouldReturnList() {
        when(questionResultService.findByUserId(1)).thenReturn(Arrays.asList(testResult));

        ResponseEntity<List<QuestionResult>> response = questionResultController.getResultsByUserId(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getResultsByQuestionId_shouldReturnList() {
        when(questionResultService.findByQuestionId(10)).thenReturn(Arrays.asList(testResult));

        ResponseEntity<List<QuestionResult>> response = questionResultController.getResultsByQuestionId(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getResultByUserAndQuestion_shouldReturnWhenExists() {
        when(questionResultService.findByUserIdAndQuestionId(1, 10)).thenReturn(Optional.of(testResult));

        ResponseEntity<QuestionResult> response = questionResultController.getResultByUserAndQuestion(1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testResult, response.getBody());
    }

    @Test
    void getResultByUserAndQuestion_shouldReturn404WhenNotExists() {
        when(questionResultService.findByUserIdAndQuestionId(99, 99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, questionResultController.getResultByUserAndQuestion(99, 99).getStatusCode());
    }

    @Test
    void getPassedResultsByUserId_shouldReturnList() {
        when(questionResultService.findByUserIdAndBestanden(1, true)).thenReturn(Arrays.asList(testResult));

        ResponseEntity<List<QuestionResult>> response = questionResultController.getPassedResultsByUserId(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getFailedResultsByUserId_shouldReturnList() {
        when(questionResultService.findByUserIdAndBestanden(1, false)).thenReturn(Collections.emptyList());

        ResponseEntity<List<QuestionResult>> response = questionResultController.getFailedResultsByUserId(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void createResult_shouldReturnCreated() {
        when(questionResultService.findByUserIdAndQuestionId(1, 10)).thenReturn(Optional.empty());
        when(questionResultService.save(any(QuestionResult.class))).thenReturn(testResult);

        ResponseEntity<?> response = questionResultController.createResult(testResult);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testResult, response.getBody());
    }

    @Test
    void createResult_shouldReturnConflictWhenAlreadyExists() {
        when(questionResultService.findByUserIdAndQuestionId(1, 10)).thenReturn(Optional.of(testResult));

        ResponseEntity<?> response = questionResultController.createResult(testResult);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(questionResultService, never()).save(any());
    }

    @Test
    void updateResult_shouldReturnUpdated() {
        when(questionResultService.findById(1)).thenReturn(Optional.of(testResult));
        when(questionResultService.save(any(QuestionResult.class))).thenReturn(testResult);

        ResponseEntity<QuestionResult> response = questionResultController.updateResult(1, testResult);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateResult_shouldReturn404WhenNotExists() {
        when(questionResultService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, questionResultController.updateResult(99, testResult).getStatusCode());
    }

    @Test
    void deleteResult_shouldReturnNoContent() {
        when(questionResultService.findById(1)).thenReturn(Optional.of(testResult));

        ResponseEntity<Void> response = questionResultController.deleteResult(1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(questionResultService).deleteById(1);
    }

    @Test
    void deleteResult_shouldReturn404WhenNotExists() {
        when(questionResultService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, questionResultController.deleteResult(99).getStatusCode());
        verify(questionResultService, never()).deleteById(any());
    }
}

