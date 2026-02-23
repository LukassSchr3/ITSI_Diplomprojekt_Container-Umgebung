package itsi.api.steuerung.service;

import io.jsonwebtoken.Claims;

public class AuthService {
    private final DatabaseService databaseService;

    public AuthService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public boolean authenticate(Claims claims, String benötigteRolle) {
        // Hier könnte eine echte Authentifizierungslogik implementiert werden
        // Zum Beispiel könnte
        return claims.get("rolle", String.class).equals(benötigteRolle);
    }
}
