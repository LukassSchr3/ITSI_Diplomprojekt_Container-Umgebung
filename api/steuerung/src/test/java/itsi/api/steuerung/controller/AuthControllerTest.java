package itsi.api.steuerung.controller;

import itsi.api.steuerung.dto.LoginRequest;
import itsi.api.steuerung.dto.LoginResponse;
import itsi.api.steuerung.dto.UserDTO;
import itsi.api.steuerung.service.DatabaseService;
import itsi.api.steuerung.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private DatabaseService databaseService;

    @InjectMocks
    private AuthController authController;

    private UserDTO testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserDTO(1, "MaxMuster", "max@test.at", "pass123",
                "5AHIT", "SCHUELER", null, null);
    }

    // --- login ---

    @Test
    void login_successfulLoginReturns200() {
        when(databaseService.findUserByEmail("max@test.at")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("jwt-token-xyz");

        ResponseEntity<LoginResponse> response = authController.login(
                new LoginRequest("max@test.at", "pass123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token-xyz");
    }

    @Test
    void login_successfulLoginResponseContainsNoPassword() {
        when(databaseService.findUserByEmail("max@test.at")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("token");

        ResponseEntity<LoginResponse> response = authController.login(
                new LoginRequest("max@test.at", "pass123"));

        assertThat(response.getBody().getUser().getPassword()).isNull();
    }

    @Test
    void login_successfulLoginResponseContainsUserData() {
        when(databaseService.findUserByEmail("max@test.at")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("token");

        ResponseEntity<LoginResponse> response = authController.login(
                new LoginRequest("max@test.at", "pass123"));

        UserDTO user = response.getBody().getUser();
        assertThat(user.getName()).isEqualTo("MaxMuster");
        assertThat(user.getEmail()).isEqualTo("max@test.at");
        assertThat(user.getRole()).isEqualTo("SCHUELER");
    }

    @Test
    void login_nullEmailReturnsBadRequest() {
        ResponseEntity<LoginResponse> response = authController.login(
                new LoginRequest(null, "pass123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Email");
    }

    @Test
    void login_emptyEmailReturnsBadRequest() {
        ResponseEntity<LoginResponse> response = authController.login(
                new LoginRequest("  ", "pass123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_nullPasswordReturnsBadRequest() {
        ResponseEntity<LoginResponse> response = authController.login(
                new LoginRequest("max@test.at", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Password");
    }

    @Test
    void login_emptyPasswordReturnsBadRequest() {
        ResponseEntity<LoginResponse> response = authController.login(
                new LoginRequest("max@test.at", ""));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_userNotFoundReturnsUnauthorized() {
        when(databaseService.findUserByEmail("nobody@test.at")).thenReturn(Optional.empty());

        ResponseEntity<LoginResponse> response = authController.login(
                new LoginRequest("nobody@test.at", "pass123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    void login_wrongPasswordReturnsUnauthorized() {
        when(databaseService.findUserByEmail("max@test.at")).thenReturn(Optional.of(testUser));

        ResponseEntity<LoginResponse> response = authController.login(
                new LoginRequest("max@test.at", "wrongpassword"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    void login_serviceThrowsExceptionReturns500() {
        when(databaseService.findUserByEmail(any())).thenThrow(new RuntimeException("DB down"));

        ResponseEntity<LoginResponse> response = authController.login(
                new LoginRequest("max@test.at", "pass123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // --- validateToken ---

    @Test
    void validateToken_validBearerTokenReturns200() {
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);

        ResponseEntity<LoginResponse> response = authController.validateToken("Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    void validateToken_invalidTokenReturnsUnauthorized() {
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        ResponseEntity<LoginResponse> response = authController.validateToken("Bearer bad-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    void validateToken_missingBearerPrefixReturnsBadRequest() {
        ResponseEntity<LoginResponse> response = authController.validateToken("just-a-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void validateToken_nullHeaderReturnsBadRequest() {
        ResponseEntity<LoginResponse> response = authController.validateToken(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void validateToken_serviceThrowsReturns500() {
        when(jwtService.isTokenValid(any())).thenThrow(new RuntimeException("JWT error"));

        ResponseEntity<LoginResponse> response = authController.validateToken("Bearer crash");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

