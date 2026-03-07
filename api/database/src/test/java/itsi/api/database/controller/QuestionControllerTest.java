package itsi.api.database.controller;

import itsi.api.database.entity.Question;
import itsi.api.database.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private QuestionController questionController;

    private Question testQuestion;

    @BeforeEach
    void setUp() {
        testQuestion = new Question();
        testQuestion.setId(1);
        testQuestion.setTaskId(10);
        testQuestion.setFrage("Was ist Java?");
        testQuestion.setAntworten("{\"a\":\"Sprache\",\"b\":\"Kaffee\"}");
        testQuestion.setBestehgrenzeProzent(BigDecimal.valueOf(50.00));
        testQuestion.setMaximalpunkte(10);
        testQuestion.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getAllQuestions_shouldReturnList() {
        when(questionService.findAll()).thenReturn(Arrays.asList(testQuestion));

        ResponseEntity<List<Question>> response = questionController.getAllQuestions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllQuestions_shouldReturnEmptyList() {
        when(questionService.findAll()).thenReturn(Collections.emptyList());

        assertTrue(questionController.getAllQuestions().getBody().isEmpty());
    }

    @Test
    void getQuestionById_shouldReturnWhenExists() {
        when(questionService.findById(1)).thenReturn(Optional.of(testQuestion));

        ResponseEntity<Question> response = questionController.getQuestionById(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testQuestion, response.getBody());
    }

    @Test
    void getQuestionById_shouldReturn404WhenNotExists() {
        when(questionService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, questionController.getQuestionById(99).getStatusCode());
    }

    @Test
    void getQuestionsByTaskId_shouldReturnList() {
        when(questionService.findByTaskId(10)).thenReturn(Arrays.asList(testQuestion));

        ResponseEntity<List<Question>> response = questionController.getQuestionsByTaskId(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getQuestionsByTaskId_shouldReturnEmptyList() {
        when(questionService.findByTaskId(99)).thenReturn(Collections.emptyList());

        assertTrue(questionController.getQuestionsByTaskId(99).getBody().isEmpty());
    }

    @Test
    void createQuestion_shouldReturnCreated() {
        when(questionService.save(any(Question.class))).thenReturn(testQuestion);

        ResponseEntity<?> response = questionController.createQuestion(testQuestion);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testQuestion, response.getBody());
    }

    @Test
    void updateQuestion_shouldReturnUpdated() {
        when(questionService.findById(1)).thenReturn(Optional.of(testQuestion));
        when(questionService.save(any(Question.class))).thenReturn(testQuestion);

        ResponseEntity<Question> response = questionController.updateQuestion(1, testQuestion);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testQuestion, response.getBody());
    }

    @Test
    void updateQuestion_shouldReturn404WhenNotExists() {
        when(questionService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, questionController.updateQuestion(99, testQuestion).getStatusCode());
    }

    @Test
    void deleteQuestion_shouldReturnNoContent() {
        when(questionService.findById(1)).thenReturn(Optional.of(testQuestion));

        ResponseEntity<Void> response = questionController.deleteQuestion(1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(questionService).deleteById(1);
    }

    @Test
    void deleteQuestion_shouldReturn404WhenNotExists() {
        when(questionService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, questionController.deleteQuestion(99).getStatusCode());
        verify(questionService, never()).deleteById(any());
    }
}

