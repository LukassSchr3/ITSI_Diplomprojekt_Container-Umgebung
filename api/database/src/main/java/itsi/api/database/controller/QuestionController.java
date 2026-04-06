package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.entity.Question;
import itsi.api.database.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:5173"})
@Tag(name = "Questions", description = "Fragen Management API")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    @Operation(summary = "Alle Fragen abrufen", description = "Gibt eine Liste aller Fragen zurück")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER')")
    public ResponseEntity<List<Question>> getAllQuestions() {
        return ResponseEntity.ok(questionService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Frage nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<Question> getQuestionById(@PathVariable Integer id) {
        return questionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Fragen nach Task-ID abrufen", description = "Gibt alle Fragen für eine bestimmte Aufgabe zurück")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<List<Question>> getQuestionsByTaskId(@PathVariable Integer taskId) {
        List<Question> questions = questionService.findByTaskId(taskId);
        return ResponseEntity.ok(questions);
    }

    @PostMapping
    @Operation(summary = "Neue Frage erstellen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER')")
    public ResponseEntity<?> createQuestion(@RequestBody Question question) {
        question.setId(null);
        try {
            log.info("Empfange createQuestion Request: taskId={}, frage={}, antworten={}", 
                     question.getTaskId(), question.getFrage(), 
                     question.getAntworten() != null ? question.getAntworten().substring(0, Math.min(50, question.getAntworten().length())) + "..." : null);
            
            // Validiere erforderliche Felder
            if (question.getTaskId() == null || question.getTaskId() <= 0) {
                String error = "taskId ist erforderlich und muss > 0 sein. Erhalten: " + question.getTaskId();
                log.warn(error);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", error));
            }
            
            if (question.getFrage() == null || question.getFrage().trim().isEmpty()) {
                String error = "Fragetext (frage) ist erforderlich. Erhalten: '" + question.getFrage() + "'";
                log.warn(error);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", error));
            }
            
            if (question.getMaximalpunkte() == null || question.getMaximalpunkte() < 0) {
                String error = "Maximalpunkte ist erforderlich und muss >= 0 sein. Erhalten: " + question.getMaximalpunkte();
                log.warn(error);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", error));
            }
            
            // Validiere antworten JSON Format
            if (question.getAntworten() != null && !question.getAntworten().trim().isEmpty()) {
                String antwortenStr = question.getAntworten().trim();
                if (!antwortenStr.startsWith("[") && !antwortenStr.startsWith("{")) {
                    String error = "Antworten müssen gültiges JSON sein (Array oder Objekt). Erhalten: " + antwortenStr.substring(0, Math.min(50, antwortenStr.length()));
                    log.warn(error);
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", error));
                }
            }
            
            // Setze Default-Werte
            if (question.getBestehgrenzeProzent() == null) {
                log.debug("Setze bestehgrenzeProzent auf default: 50");
                question.setBestehgrenzeProzent(BigDecimal.valueOf(50.00));
            }
            
            // Setze ID auf null (wird auto-generiert)

            
            // Setze createdAt auf null (wird durch @PrePersist gesetzt)
            question.setCreatedAt(null);
            
            log.info("Speichere Frage in DB...");
            Question savedQuestion = questionService.save(question);
            log.info("Frage erfolgreich erstellt mit ID: {}", savedQuestion.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedQuestion);
        } catch (Exception e) {
            log.error("Fehler beim Erstellen der Frage", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "error", "Fehler beim Erstellen der Frage: " + e.getMessage(),
                        "details", e.getClass().getSimpleName()
                    ));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Frage aktualisieren")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER')")
    public ResponseEntity<Question> updateQuestion(@PathVariable Integer id, @RequestBody Question question) {
        if (!questionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        question.setId(id);
        return ResponseEntity.ok(questionService.save(question));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Frage löschen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Integer id) {
        if (!questionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        questionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
