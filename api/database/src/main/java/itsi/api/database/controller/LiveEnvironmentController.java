package itsi.api.database.controller;

import itsi.api.database.entity.LiveEnvironment;
import itsi.api.database.service.LiveEnvironmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/live-environments")
public class LiveEnvironmentController {
    @Autowired
    private LiveEnvironmentService service;

    @GetMapping
    public List<LiveEnvironment> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LiveEnvironment> getById(@PathVariable Long id) {
        Optional<LiveEnvironment> env = service.findById(id);
        return env.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public LiveEnvironment create(@RequestBody LiveEnvironment env) {
        return service.save(env);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LiveEnvironment> update(@PathVariable Long id, @RequestBody LiveEnvironment env) {
        if (!service.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        env.setId(id);
        return ResponseEntity.ok(service.save(env));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!service.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/max-vnc-port")
    public Integer getMaxVncPort() {
        return service.getMaxVncPort();
    }
}
