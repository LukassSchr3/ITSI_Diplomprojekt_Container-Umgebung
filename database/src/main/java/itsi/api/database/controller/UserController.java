package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.dto.CreateUserDTO;
import itsi.api.database.dto.UpdateUserDTO;
import itsi.api.database.dto.UserDTO;
import itsi.api.database.entity.User;
import itsi.api.database.mapper.UserMapper;
import itsi.api.database.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User Management API")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    @Operation(summary = "Alle Benutzer abrufen", description = "Gibt eine Liste aller Benutzer zurück (ohne Passwörter)")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.findAll().stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Benutzer nach ID abrufen", description = "Gibt Benutzerdetails zurück (ohne Passwort)")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Integer id) {
        return userService.findById(id)
                .map(userMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Benutzer nach Namen abrufen", description = "Gibt Benutzerdetails zurück (ohne Passwort)")
    public ResponseEntity<UserDTO> getUserByName(@PathVariable String name) {
        return userService.findByName(name)
                .map(userMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Benutzer nach E-Mail abrufen", description = "Gibt Benutzerdetails zurück (ohne Passwort)")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        return userService.findByEmail(email)
                .map(userMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Neuen Benutzer erstellen", description = "Erstellt einen neuen Benutzer und gibt die Details zurück (ohne Passwort)")
    public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserDTO createUserDTO) {
        User user = userMapper.toEntity(createUserDTO);
        User savedUser = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDTO(savedUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Benutzer aktualisieren", description = "Aktualisiert Benutzerdaten (Passwort ist optional)")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Integer id, @RequestBody UpdateUserDTO updateUserDTO) {
        return userService.findById(id)
                .map(existingUser -> {
                    userMapper.updateEntity(existingUser, updateUserDTO);
                    User updatedUser = userService.save(existingUser);
                    return ResponseEntity.ok(userMapper.toDTO(updatedUser));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Benutzer löschen")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        if (!userService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
