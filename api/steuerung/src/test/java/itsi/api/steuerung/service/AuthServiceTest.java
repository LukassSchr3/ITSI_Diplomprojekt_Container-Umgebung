package itsi.api.steuerung.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest {

    private AuthService authService;
    private DatabaseService databaseService;

    private static final String SECRET =
            "c29tZS1yYW5kb20tc2VjcmV0LWtleS1mb3Itand0LWdlbmVyYXRpb24tdGhhdC1pcy1sb25nLWVub3VnaA==";

    @BeforeEach
    void setUp() {
        databaseService = null; // AuthService nutzt databaseService nicht in authenticate()
        authService = new AuthService(databaseService);
    }

    private Claims buildClaims(String rolle) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("rolle", rolle);

        String token = Jwts.builder()
                .claims(claimsMap)
                .subject("testuser")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000L))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    void authenticateMatchingRoleReturnsTrue() {
        Claims claims = buildClaims("ADMIN");
        assertThat(authService.authenticate(claims, "ADMIN")).isTrue();
    }

    @Test
    void authenticateWrongRoleReturnsFalse() {
        Claims claims = buildClaims("SCHUELER");
        assertThat(authService.authenticate(claims, "ADMIN")).isFalse();
    }

    @Test
    void authenticateSchuelerRoleMatches() {
        Claims claims = buildClaims("SCHUELER");
        assertThat(authService.authenticate(claims, "SCHUELER")).isTrue();
    }

    @Test
    void authenticateLehrerRoleMatches() {
        Claims claims = buildClaims("LEHRER");
        assertThat(authService.authenticate(claims, "LEHRER")).isTrue();
    }

    @Test
    void authenticateEmptyRolleReturnsFalse() {
        Claims claims = buildClaims("SCHUELER");
        assertThat(authService.authenticate(claims, "")).isFalse();
    }

    @Test
    void authenticateCaseSensitive() {
        Claims claims = buildClaims("admin");
        assertThat(authService.authenticate(claims, "ADMIN")).isFalse();
    }
}

