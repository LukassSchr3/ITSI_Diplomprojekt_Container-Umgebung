package itsi.api.database.controller;

import itsi.api.database.entity.LiveEnvironment;
import itsi.api.database.service.LiveEnvironmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/live-environments")
public class LiveEnvironmentController {
    @Autowired
    private LiveEnvironmentService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN')")
    public List<LiveEnvironment> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LiveEnvironment> getById(@PathVariable Long id) {
        Optional<LiveEnvironment> env = service.findById(id);
        return env.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN')")
    public LiveEnvironment create(@RequestBody LiveEnvironment env) {
        return service.save(env);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN')")
    public ResponseEntity<LiveEnvironment> update(@PathVariable Long id, @RequestBody LiveEnvironment env) {
        if (!service.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        env.setId(id);
        return ResponseEntity.ok(service.save(env));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!service.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/max-vnc-port")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN')")
    public Integer getMaxVncPort() {
        return service.getMaxVncPort();
    }

    // Neue Endpunkte nach userId
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN') or @securityService.isOwner(#userId)")
    public ResponseEntity<LiveEnvironment> getByUserId(@PathVariable Long userId) {
        Optional<LiveEnvironment> env = service.findByUserId(userId);
        return env.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN')")
    public ResponseEntity<LiveEnvironment> createByUserId(@PathVariable Long userId, @RequestBody LiveEnvironment env) {
        // Wenn bereits ein LiveEnvironment für die userId existiert, geben wir 409 zurück
        if (service.findByUserId(userId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        env.setUserId(userId);
        LiveEnvironment saved = service.save(env);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(String.format("/api/live-environments/%d", saved.getId())));
        return new ResponseEntity<>(saved, headers, HttpStatus.CREATED);
    }

    @PutMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN') or @securityService.isOwner(#userId)")
    public ResponseEntity<LiveEnvironment> updateByUserId(@PathVariable Long userId, @RequestBody LiveEnvironment env) {
        Optional<LiveEnvironment> existing = service.findByUserId(userId);
        if (!existing.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        LiveEnvironment toSave = existing.get();
        // Aktualisiere Felder (behalte id und userId)
        toSave.setVncPort(env.getVncPort());
        toSave.setVncHost(env.getVncHost());
        toSave.setVncPassword(env.getVncPassword());
        toSave.setStatus(env.getStatus());

        return ResponseEntity.ok(service.save(toSave));
    }

    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN')")
    public ResponseEntity<Void> deleteByUserId(@PathVariable Long userId) {
        Optional<LiveEnvironment> existing = service.findByUserId(userId);
        if (!existing.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        service.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
