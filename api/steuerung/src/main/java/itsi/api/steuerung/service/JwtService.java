package itsi.api.steuerung.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import itsi.api.steuerung.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String generateToken(UserDTO user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("rolle", user.getRole());
        claims.put("klasse", user.getClassName());
        claims.put("ablaufJahr", user.getExpiredAt());
        claims.put("email", user.getEmail());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            String rolle = claims.get("rolle", String.class);
            return "SCHUELER".equals(rolle) || "LEHRER".equals(rolle) || "ADMIN".equals(rolle);
        } catch (Exception e) {
            return false;
        }
    }
}

