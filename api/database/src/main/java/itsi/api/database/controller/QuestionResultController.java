package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.entity.QuestionResult;
import itsi.api.database.service.QuestionResultService;
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
@RequestMapping("/api/question-results")
@RequiredArgsConstructor
@Tag(name = "Question Results", description = "Fragen Ergebnisse Management API")
public class QuestionResultController {

    private final QuestionResultService questionResultService;

    @GetMapping
    @Operation(summary = "Alle Ergebnisse abrufen")
    public ResponseEntity<List<QuestionResult>> getAllResults() {
        return ResponseEntity.ok(questionResultService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ergebnis nach ID abrufen")
    public ResponseEntity<QuestionResult> getResultById(@PathVariable Integer id) {
        return questionResultService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Alle Ergebnisse eines Users abrufen")
    public ResponseEntity<List<QuestionResult>> getResultsByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(questionResultService.findByUserId(userId));
    }

    @GetMapping("/question/{questionId}")
    @Operation(summary = "Alle Ergebnisse einer Frage abrufen")
    public ResponseEntity<List<QuestionResult>> getResultsByQuestionId(@PathVariable Integer questionId) {
        return ResponseEntity.ok(questionResultService.findByQuestionId(questionId));
    }

    @GetMapping("/user/{userId}/question/{questionId}")
    @Operation(summary = "Ergebnis eines Users für eine spezifische Frage abrufen")
    public ResponseEntity<QuestionResult> getResultByUserAndQuestion(
            @PathVariable Integer userId,
            @PathVariable Integer questionId) {
        return questionResultService.findByUserIdAndQuestionId(userId, questionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}/passed")
    @Operation(summary = "Alle bestandenen Fragen eines Users abrufen")
    public ResponseEntity<List<QuestionResult>> getPassedResultsByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(questionResultService.findByUserIdAndBestanden(userId, true));
    }

    @GetMapping("/user/{userId}/failed")
    @Operation(summary = "Alle nicht bestandenen Fragen eines Users abrufen")
    public ResponseEntity<List<QuestionResult>> getFailedResultsByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(questionResultService.findByUserIdAndBestanden(userId, false));
    }

    @PostMapping
    @Operation(summary = "Neues Ergebnis speichern", description = "Speichert das Ergebnis einer beantworteten Frage")
    public ResponseEntity<?> createResult(@RequestBody QuestionResult result) {
        try {
            // Check if result already exists
            if (questionResultService.findByUserIdAndQuestionId(result.getUserId(), result.getQuestionId()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "User has already answered this question"));
            }

            QuestionResult savedResult = questionResultService.save(result);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedResult);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create result: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ergebnis aktualisieren")
    public ResponseEntity<QuestionResult> updateResult(@PathVariable Integer id, @RequestBody QuestionResult result) {
        if (!questionResultService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        result.setId(id);
        return ResponseEntity.ok(questionResultService.save(result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Ergebnis löschen")
    public ResponseEntity<Void> deleteResult(@PathVariable Integer id) {
        if (!questionResultService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        questionResultService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
