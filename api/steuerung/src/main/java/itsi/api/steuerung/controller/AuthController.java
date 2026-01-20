package itsi.api.steuerung.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.steuerung.dto.LoginRequest;
import itsi.api.steuerung.dto.LoginResponse;
import itsi.api.steuerung.dto.UserDTO;
import itsi.api.steuerung.service.DatabaseService;
import itsi.api.steuerung.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and JWT token generation")
@Slf4j
public class AuthController {

    private final JwtService jwtService;
    private final DatabaseService databaseService;

    public AuthController(JwtService jwtService, DatabaseService databaseService) {
        this.jwtService = jwtService;
        this.databaseService = databaseService;
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates a user by userId and returns a JWT token")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for userId: {}", loginRequest.getUserId());

        try {
            // Validate request
            if (loginRequest.getUserId() == null) {
                log.warn("Login failed: userId is null");
                return ResponseEntity.badRequest().body(
                    new LoginResponse(false, "UserId is required", null, null)
                );
            }

            // Fetch user from database
            UserDTO user = databaseService.getUserById(loginRequest.getUserId());

            if (user == null) {
                log.warn("Login failed: User not found with id: {}", loginRequest.getUserId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new LoginResponse(false, "User not found", null, null)
                );
            }

            // Generate JWT token
            String token = jwtService.generateToken(user);
            log.info("Login successful for user: {} (id: {})", user.getName(), user.getId());

            // Return success response with token and user data
            return ResponseEntity.ok(
                new LoginResponse(true, "Login successful", token, user)
            );

        } catch (Exception e) {
            log.error("Login error for userId: {}", loginRequest.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new LoginResponse(false, "An error occurred during login: " + e.getMessage(), null, null)
            );
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate Token", description = "Validates if a JWT token is valid")
    public ResponseEntity<LoginResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(
                    new LoginResponse(false, "Invalid authorization header", null, null)
                );
            }

            String token = authHeader.substring(7);
            boolean isValid = jwtService.isTokenValid(token);

            if (isValid) {
                return ResponseEntity.ok(
                    new LoginResponse(true, "Token is valid", null, null)
                );
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new LoginResponse(false, "Token is invalid or expired", null, null)
                );
            }
        } catch (Exception e) {
            log.error("Token validation error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new LoginResponse(false, "Error validating token: " + e.getMessage(), null, null)
            );
        }
    }
}
