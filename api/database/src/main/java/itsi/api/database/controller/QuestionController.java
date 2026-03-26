package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.entity.Question;
import itsi.api.database.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Tag(name = "Questions", description = "Fragen Management API")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    @Operation(summary = "Alle Fragen abrufen", description = "Gibt eine Liste aller Fragen zurück")
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(questionService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Frage nach ID abrufen")
    public ResponseEntity<Question> getQuestionById(@PathVariable Integer id) {
        return questionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Fragen nach Task-ID abrufen", description = "Gibt alle Fragen für eine bestimmte Aufgabe zurück")
    public ResponseEntity<List<Question>> getQuestionsByTaskId(@PathVariable Integer taskId) {
        List<Question> questions = questionService.findByTaskId(taskId);
        return ResponseEntity.ok(questions);
    }

    @PostMapping
    @Operation(summary = "Neue Frage erstellen")
    public ResponseEntity<?> createQuestion(@RequestBody Question question) {
        try {
            Question savedQuestion = questionService.save(question);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedQuestion);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create question: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Frage aktualisieren")
    public ResponseEntity<Question> updateQuestion(@PathVariable Integer id, @RequestBody Question question) {
        if (!questionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        question.setId(id);
        return ResponseEntity.ok(questionService.save(question));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Frage löschen")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Integer id) {
        if (!questionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        questionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
