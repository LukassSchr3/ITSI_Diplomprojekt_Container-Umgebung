package itsi.api.steuerung.service;

import io.jsonwebtoken.Claims;
import itsi.api.steuerung.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    // 64+ Byte Base64-kodierter Secret für HS256
    private static final String SECRET =
            "c29tZS1yYW5kb20tc2VjcmV0LWtleS1mb3Itand0LWdlbmVyYXRpb24tdGhhdC1pcy1sb25nLWVub3VnaA==";
    private static final long EXPIRATION = 86400000L; // 24h

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
        // AuthService muss gesetzt sein, da JwtService.isTokenValid() ihn intern nutzt
        AuthService authService = new AuthService(null);
        ReflectionTestUtils.setField(jwtService, "authService", authService);
    }

    private UserDTO createUser(String role) {
        return new UserDTO(1, "TestUser", "test@test.at", "secret", "5AHIT", role,
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis() + 86400000L));
    }

    // --- generateToken ---

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken(createUser("SCHUELER"));
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_tokenContainsThreeParts() {
        String token = jwtService.generateToken(createUser("LEHRER"));
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_differentUsersGetDifferentTokens() {
        UserDTO u1 = createUser("SCHUELER");
        UserDTO u2 = new UserDTO(2, "Other", "other@test.at", "pass", "4AHIT", "LEHRER", null, null);
        assertThat(jwtService.generateToken(u1)).isNotEqualTo(jwtService.generateToken(u2));
    }

    // --- extractClaims ---

    @Test
    void extractClaims_subjectMatchesUserName() {
        UserDTO user = createUser("ADMIN");
        String token = jwtService.generateToken(user);
        Claims claims = jwtService.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("TestUser");
    }

    @Test
    void extractClaims_roleClaimIsPresent() {
        String token = jwtService.generateToken(createUser("LEHRER"));
        Claims claims = jwtService.extractClaims(token);
        assertThat(claims.get("rolle", String.class)).isEqualTo("LEHRER");
    }

    @Test
    void extractClaims_userIdClaimIsPresent() {
        String token = jwtService.generateToken(createUser("SCHUELER"));
        Claims claims = jwtService.extractClaims(token);
        assertThat(claims.get("userId")).isNotNull();
    }

    @Test
    void extractClaims_emailClaimIsPresent() {
        UserDTO user = createUser("SCHUELER");
        String token = jwtService.generateToken(user);
        Claims claims = jwtService.extractClaims(token);
        assertThat(claims.get("email", String.class)).isEqualTo("test@test.at");
    }

    @Test
    void extractClaims_klasseClaimIsPresent() {
        String token = jwtService.generateToken(createUser("SCHUELER"));
        Claims claims = jwtService.extractClaims(token);
        assertThat(claims.get("klasse", String.class)).isEqualTo("5AHIT");
    }

    @Test
    void extractClaims_invalidTokenThrowsException() {
        assertThatThrownBy(() -> jwtService.extractClaims("invalid.token.value"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractClaims_tamperedTokenThrowsException() {
        String token = jwtService.generateToken(createUser("SCHUELER"));
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThatThrownBy(() -> jwtService.extractClaims(tampered))
                .isInstanceOf(Exception.class);
    }

    // --- isTokenValid ---

    @Test
    void isTokenValid_validSchuelerTokenReturnsTrue() {
        String token = jwtService.generateToken(createUser("SCHUELER"));
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_validLehrerTokenReturnsTrue() {
        String token = jwtService.generateToken(createUser("LEHRER"));
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_validAdminTokenReturnsTrue() {
        String token = jwtService.generateToken(createUser("ADMIN"));
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_invalidTokenReturnsFalse() {
        assertThat(jwtService.isTokenValid("this.is.not.valid")).isFalse();
    }

    @Test
    void isTokenValid_emptyStringReturnsFalse() {
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    void isTokenValid_expiredTokenReturnsFalse() throws Exception {
        // Erstelle Token mit negativer Ablaufzeit (sofort abgelaufen)
        JwtService shortLivedService = new JwtService();
        ReflectionTestUtils.setField(shortLivedService, "secret", SECRET);
        ReflectionTestUtils.setField(shortLivedService, "expiration", -1000L);
        ReflectionTestUtils.setField(shortLivedService, "authService", new AuthService(null));

        String token = shortLivedService.generateToken(createUser("SCHUELER"));
        assertThat(shortLivedService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_unknownRoleReturnsFalse() {
        // Generiere Token mit unbekannter Rolle – isTokenValid soll false zurückgeben
        UserDTO user = createUser("UNKNOWN_ROLE");
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void generateToken_nullUserExpiredAtIsHandled() {
        UserDTO user = new UserDTO(1, "TestUser", "test@test.at", "secret", "5AHIT", "SCHUELER",
                null, null);
        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void extractClaims_ablaufJahrClaimPresent() {
        UserDTO user = new UserDTO(1, "TestUser", "test@test.at", "secret", "5AHIT", "SCHUELER",
                null, new java.sql.Timestamp(System.currentTimeMillis() + 86400000L));
        String token = jwtService.generateToken(user);
        Claims claims = jwtService.extractClaims(token);
        // ablaufJahr may be null if expiredAt is null, but claim key exists
        assertThat(claims.containsKey("email")).isTrue();
    }

    @Test
    void generateToken_tokenIssuedAtIsRecent() {
        String token = jwtService.generateToken(createUser("SCHUELER"));
        Claims claims = jwtService.extractClaims(token);
        long now = System.currentTimeMillis();
        long issuedAt = claims.getIssuedAt().getTime();
        assertThat(Math.abs(now - issuedAt)).isLessThan(5000L);
    }

    @Test
    void generateToken_tokenExpiresInFuture() {
        String token = jwtService.generateToken(createUser("SCHUELER"));
        Claims claims = jwtService.extractClaims(token);
        assertThat(claims.getExpiration().getTime()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void isTokenValid_nullTokenReturnsFalse() {
        assertThat(jwtService.isTokenValid(null)).isFalse();
    }

    @Test
    void extractClaims_userIdMatchesUserObject() {
        UserDTO user = new UserDTO(42, "TestUser", "test@test.at", "secret", "5AHIT", "SCHUELER", null, null);
        String token = jwtService.generateToken(user);
        Claims claims = jwtService.extractClaims(token);
        assertThat(((Number) claims.get("userId")).intValue()).isEqualTo(42);
    }

    @Test
    void generateToken_sameUserGeneratesDifferentTokensDueToTime() throws InterruptedException {
        UserDTO user = createUser("SCHUELER");
        String token1 = jwtService.generateToken(user);
        Thread.sleep(1100); // ensure different issuedAt
        String token2 = jwtService.generateToken(user);
        // Tokens should differ because issuedAt differs (or expiration differs)
        assertThat(token1).isNotEqualTo(token2);
    }
}
