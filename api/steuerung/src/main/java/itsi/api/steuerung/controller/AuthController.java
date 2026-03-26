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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(summary = "User Login", description = "Authenticates a user by email and password, returns a JWT token")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        try {
            // Validate request
            if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
                log.warn("Login failed: email is null or empty");
                return ResponseEntity.badRequest().body(
                    new LoginResponse(false, "Email is required", null, null)
                );
            }

            if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                log.warn("Login failed: password is null or empty");
                return ResponseEntity.badRequest().body(
                    new LoginResponse(false, "Password is required", null, null)
                );
            }

            // Fetch user from database
            if(databaseService.findUserByEmail(loginRequest.getEmail()).isEmpty()) {
                log.warn("Login failed: User not found with email: {}", loginRequest.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new LoginResponse(false, "Invalid email or password", null, null)
                );
            }
            UserDTO user = databaseService.findUserByEmail(loginRequest.getEmail()).get();

            log.info("User Passwort {}",user.getPassword());

            // Validate password (simple comparison - in production use BCrypt!)
            if (!loginRequest.getPassword().equals(user.getPassword())) {
                log.warn("Login failed: Invalid password for email: {}", loginRequest.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new LoginResponse(false, "Invalid email or password", null, null)
                );
            }

            // Generate JWT token
            String token = jwtService.generateToken(user);
            log.info("Login successful for user: {} (id: {})", user.getName(), user.getId());

            // Return success response with token and user data (without password!)
            UserDTO safeUser = new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                null, // Don't send password back!
                user.getClassName(),
                user.getRole(),
                user.getCreatedAt(),
                user.getExpiredAt()
            );

            return ResponseEntity.ok(
                new LoginResponse(true, "Login successful", token, safeUser)
            );

        } catch (Exception e) {
            log.error("Login error for email: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new LoginResponse(false, "An error occurred during login", null, null)
            );
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate Token", description = "Validates if a JWT token is valid")
    public ResponseEntity<LoginResponse> validateToken(@RequestHeader() String authHeader) {
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
