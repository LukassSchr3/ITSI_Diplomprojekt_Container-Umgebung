# Backend Authentication Feature - Detaillierte Dokumentation

## 📋 Überblick

Das Backend implementiert ein **JWT-basiertes (JSON Web Token) Authentication System** für die Steuerung-Middleware. Das System ermöglicht es Benutzern, sich anzumelden und basierend auf ihren Rollen Zugriff auf geschützte Endpoints zu erhalten.

---

## 🏗️ Architektur & Komponenten

### 1. **JWT Service** (`JwtService.java`)
Kern-Service für Token-Management.

#### Hauptaufgaben:
- **Token Generierung**: Erstellt signierte JWT Tokens mit Benutzerdaten
- **Token Validierung**: Verifiziert die Signatur und Claims
- **Claims Extraktion**: Extrahiert Benutzerdaten aus Tokens

#### Key Methods:

```java
// Token mit User-Daten generieren
public String generateToken(UserDTO user)
```
- Erzeugt einen HS256-signierten JWT Token
- Enthält folgende Claims:
  - `userId`: Eindeutige Benutzer-ID
  - `rolle`: Benutzerrolle (SCHUELER, LEHRER, ADMIN)
  - `klasse`: Schulklasse des Benutzers
  - `ablaufJahr`: Ablaufjahr der Berechtigung
  - `email`: E-Mail-Adresse
  - `subject`: Benutzername
  - `issuedAt`: Ausstellungszeitpunkt
  - `expiration`: Gültigkeitsdauer

**Token Gültigkeitsdauer**: 86.400.000 ms = **24 Stunden**

```java
// Token validieren
public boolean isTokenValid(String token)
```
- Verifiziert JWT-Signatur
- Prüft Rollen-Authentifizierung (SCHUELER, LEHRER, ADMIN)
- Rückgabewert: `true` wenn gültig, `false` wenn abgelaufen oder ungültig

```java
// Claims aus Token extrahieren
public Claims extractClaims(String token)
```
- Parst und verifiziert den Token
- Gibt Payload-Daten zurück

#### Signierung:
- **Algorithmus**: HMAC-SHA256 (HS256)
- **Secret**: Base64-kodiert aus `application.properties`
- **Secret-Wert**: `c29tZS1yYW5kb20tc2VjcmV0LWtleS1mb3Itand0LWdlbmVyYXRpb24tdGhhdC1pcy1sb25nLWVub3VnaA==`

---

### 2. **Auth Service** (`AuthService.java`)
Delegiert Authentifizierungs-Logik.

```java
public boolean authenticate(Claims claims, String benötigteRolle)
```
- **Einfache Rollen-Validierung**: Vergleicht Rollen-Claim mit erforderter Rolle
- **Ist anweichbar**: Kann für komplexe Authorization-Logik erweitert werden
- **Verwendung in `JwtService`**: Prüft ob Benutzer eine gültige Rolle hat

---

### 3. **Auth Controller** (`AuthController.java`)
REST-Endpoints für Authentifizierung.

#### Endpoint 1: `POST /api/auth/login`
**Zweck**: Benutzer-Authentifizierung und Token-Generierung

**Request-Body**:
```json
{
  "email": "user@example.com",
  "password": "passwort123"
}
```

**Response (Erfolg - 200 OK)**:
```json
{
  "success": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": "123",
    "name": "Max Mustermann",
    "email": "user@example.com",
    "password": null,
    "className": "5AHIT",
    "role": "SCHUELER",
    "createdAt": "2025-01-15",
    "expiredAt": "2026-06-30"
  }
}
```

**Response (Fehler - 401 Unauthorized)**:
```json
{
  "success": false,
  "message": "Invalid email or password",
  "token": null,
  "user": null
}
```

**Validierungen**:
1. Email ist nicht leer/null
2. Passwort ist nicht leer/null
3. Benutzer existiert in Datenbank
4. Passwort stimmt überein (Plain-Text Vergleich - ⚠️ **SICHERHEITSPROBLEM!**)

**Sicherheitsmerkmale**:
- ✅ Passwort wird nicht in Response zurückgesendet
- ❌ Passwort wird im Klartext gespeichert und verglichen (sollte BCrypt verwenden)
- ✅ Generisches Error-Message ("Invalid email or password") - verhindert User-Enumeration
- ✅ Logging für Audit-Trail

#### Endpoint 2: `GET /api/auth/validate`
**Zweck**: Token-Validierung

**Request**:
```
GET /api/auth/validate
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response (Gültig - 200 OK)**:
```json
{
  "success": true,
  "message": "Token is valid",
  "token": null,
  "user": null
}
```

**Response (Ungültig - 401 Unauthorized)**:
```json
{
  "success": false,
  "message": "Token is invalid or expired",
  "token": null,
  "user": null
}
```

**Validierungen**:
1. Authorization Header existiert und startet mit "Bearer "
2. Token-Signatur ist gültig
3. Token ist nicht abgelaufen

---

### 4. **JWT Authentication Filter** (`JwtAuthenticationFilter.java`)
Spring Security Filter zur automatischen Token-Verarbeitung.

```java
protected void doFilterInternal(HttpServletRequest request, 
                                HttpServletResponse response, 
                                FilterChain filterChain)
```

**Ablauf für jeden Request**:
1. Extrahiert Authorization Header
2. Prüft auf "Bearer " Präfix
3. Validiert Token mit `JwtService.isTokenValid()`
4. Extrahiert Claims aus Token
5. Erstellt `UsernamePasswordAuthenticationToken` mit:
   - Subject (Benutzername)
   - Authorities (Benutzerrolle)
   - Details (Alle Token-Claims)
6. Setzt SecurityContext
7. Leitet Request an nächsten Filter weiter

**Wichtig**: Filter wird für **jeden Request** ausgeführt (OncePerRequestFilter)

---

### 5. **Security Config** (`SecurityConfig.java`)
Spring Security Konfiguration.

**CSRF Protection**: ❌ Deaktiviert (Stateless API)

**Session Management**: `STATELESS`
- Keine Server-seitigen Sessions
- Jeder Request enthält seinen eigenen Token

**Endpoint-Autorisierung**:
```
✅ ÖFFENTLICH (permitAll):
   - /api-docs/**          (OpenAPI Dokumentation)
   - /swagger-ui/**        (Swagger UI)
   - /swagger-ui.html      (Swagger UI HTML)
   - /**                   (Alle anderen - WARNUNG! Sollte eingeschränkt werden)
   - /api/info             (Info Endpoint)
   - /api/auth/**          (Login, Validate)

🔒 AUTHENTIFIZIERT (authenticated):
   - Alle anderen Endpoints
```

**Filter-Reihenfolge**:
```
JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter → Standard Filters
```

---

## 🔐 Authentifizierungs-Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. USER LOGIN                                                     │
│ POST /api/auth/login                                              │
│ { "email": "user@example.com", "password": "secret" }             │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. AUTH CONTROLLER - VALIDIERUNG                                 │
│ • Email & Password nicht leer?                                   │
│ • User in Datenbank?                                             │
│ • Passwort stimmt?                                               │
└────────────────────────┬────────────────────────────────────────┘
                         │
                    ✅ JA│ ❌ NEIN → 401 Unauthorized
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. TOKEN GENERATION                                               │
│ JwtService.generateToken(UserDTO)                                │
│ • Erstelle Claims (userId, rolle, klasse, email, etc.)          │
│ • Signiere mit HS256 Secret                                      │
│ • Setze Gültigkeitsdauer (24h)                                   │
│ • Rückgabe: Signierter JWT Token                                │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. LOGIN RESPONSE                                                 │
│ 200 OK: { token, user (ohne password) }                           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 5. GESCHÜTZTE ENDPOINT ANFRAGE                                   │
│ GET /api/protected/resource                                      │
│ Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...                    │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. JWT AUTHENTICATION FILTER                                     │
│ • Extrahiere "Authorization" Header                              │
│ • Parse Token nach "Bearer " Präfix                              │
│ • Validiere Signatur & Ablaufzeit                                │
│ • Extrahiere Claims                                              │
└────────────────────────┬────────────────────────────────────────┘
                         │
                    ✅ GÜLTIG│ ❌ UNGÜLTIG → 401 Unauthorized
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. SECURITY CONTEXT SETUP                                        │
│ • Erstelle UsernamePasswordAuthenticationToken                   │
│ • Setze Principal (Benutzername)                                 │
│ • Setze Authorities (Benutzerrolle)                              │
│ • Setze Details (Token Claims)                                   │
│ • Speichere in SecurityContextHolder                             │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8. ENDPOINT AUSFÜHRUNG                                            │
│ • SecurityContext enthält Authentifizierungsdaten                │
│ • Endpoint kann Benutzerinfo abrufen:                            │
│   - @AuthenticationPrincipal String username                     │
│   - @AuthenticationPrincipal(expression="details") Claims claims │
│ • Controller-Logik führt aus                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📦 Data Transfer Objects (DTOs)

### LoginRequest
```java
{
  "email": "user@example.com",
  "password": "passwort123"
}
```

### LoginResponse
```java
{
  "success": boolean,
  "message": String,
  "token": String (JWT Token oder null),
  "user": UserDTO (ohne Passwort)
}
```

### UserDTO
```java
{
  "id": "unique-id",
  "name": "Max Mustermann",
  "email": "user@example.com",
  "password": null,           // Nie in Response!
  "className": "5AHIT",
  "role": "SCHUELER|LEHRER|ADMIN",
  "createdAt": "2025-01-15",
  "expiredAt": "2026-06-30"
}
```

---

## ⚙️ Konfiguration (`application.properties`)

```properties
# JWT Secret (Base64 encoded)
jwt.secret=c29tZS1yYW5kb20tc2VjcmV0LWtleS1mb3Itand0LWdlbmVyYXRpb24tdGhhdC1pcy1sb25nLWVub3VnaA==

# Token Gültigkeitsdauer in Millisekunden (24h)
jwt.expiration=86400000
```

**Secret dekodiert (Base64)**:
```
some-random-secret-key-for-jwt-generation-that-is-long-enough
```

---

## 👥 Benutzerrollen

Das System unterstützt **3 Rollen**:

| Rolle | Beschreibung | Berechtigungen |
|-------|------------|-----------------|
| **SCHUELER** | Schüler | Lesezugriff auf eigene Daten |
| **LEHRER** | Lehrer | Verwaltung von Schülerdaten |
| **ADMIN** | Administrator | Vollzugriff auf alle Systeme |

**Validierung**: Die Rolle wird im JWT Token gespeichert und bei jedem Request überprüft.

---

## 🔍 Token-Struktur

Ein generierter JWT Token hat folgende Struktur:

```
eyJhbGciOiJIUzI1NiJ9.eyJyb2xsZSI6IlNDSFVFTEVSIiwiam...EzIn0.signature
│                                  │                          │
Header                             Payload                    Signature
```

### Header (Base64 decodiert):
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

### Payload (Base64 decodiert):
```json
{
  "role": "SCHUELER",
  "userId": "12345",
  "className": "5AHIT",
  "ablaufJahr": "2026-06-30",
  "email": "user@example.com",
  "sub": "Max Mustermann",
  "iat": 1705334400000,
  "exp": 1705420800000
}
```

### Signature:
```
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

---

## ⚠️ Sicherheitsprobleme & Verbesserungen

### 🔴 Kritische Probleme

#### 1. **Plaintext Passwörter** (KRITISCH!)
**Problem**: Passwörter werden im Klartext gespeichert und verglichen.
```java
// Aktuell (unsicher):
if (!loginRequest.getPassword().equals(user.getPassword())) { ... }
```

**Lösung**: BCrypt Hash verwenden
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

// Password hashen beim Speichern:
user.setPassword(passwordEncoder.encode("plaintext-password"));

// Password validieren beim Login:
if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) { ... }
```

#### 2. **Zu breite CORS/Autorisierung**
**Problem**: Wildcard `/**` erlaubt Zugriff auf alle Endpoints
```java
.requestMatchers("/**").permitAll()  // ❌ ZU BREIT!
```

**Lösung**: Nur spezifische Endpoints erlauben
```java
.requestMatchers(
    "/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/api/info",
    "/api/auth/**"
).permitAll()
```

#### 3. **Schwacher Secret-Key**
**Problem**: JWT Secret ist öffentlich bekannt und zu kurz.

**Lösung**: 
- Mindestens 256-Bit (32 Bytes) Secret verwenden
- In Environment-Variablen speichern, nicht in Properties

```properties
jwt.secret=${JWT_SECRET}  # Aus Umgebungsvariable laden
```

### 🟡 Mittlere Probleme

#### 4. **Keine Rate Limiting**
**Problem**: Brute-Force Attacken möglich auf Login-Endpoint.

**Lösung**: Rate Limiting implementieren (z.B. Spring Rate Limiter)

#### 5. **Keine Refresh Token**
**Problem**: Tokens sind 24h lang gültig, können nicht erneuert werden.

**Lösung**: Refresh Token Mechanismus implementieren
```
- Short-lived Access Token (15 min)
- Long-lived Refresh Token (7 Tage)
- Refresh Endpoint: POST /api/auth/refresh
```

#### 6. **Keine Token Invalidation (Logout)**
**Problem**: Tokens sind gültig bis zur Expiration, nicht früher invalidierbar.

**Lösung**: Token Blacklist implementieren
```java
@Service
public class TokenBlacklistService {
    private Set<String> blacklist = new HashSet<>();
    
    public void blacklist(String token) {
        blacklist.add(token);
    }
    
    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }
}
```

#### 7. **Plaintext Logging von Passwörtern**
**Problem**: Password wird geloggt
```java
log.info("User Passwort {}", user.getPassword());  // ❌ NIEMALS!
```

**Lösung**: Entfernen
```java
// Nicht loggen!
log.info("Login attempt for user: {}", user.getId());
```

#### 8. **Keine HTTPS Erzwingung**
**Problem**: Tokens könnten abgefangen werden.

**Lösung**: HTTPS erzwingen + Secure Cookie Flag

### 🟢 Best Practices (Implementiert)

✅ JWT Signierung mit HS256  
✅ Ablaufzeit (Expiration)  
✅ Generische Error-Messages (Anti User-Enumeration)  
✅ Authorization Header Parsing  
✅ Filter für alle Requests  
✅ Rollen-basierte Autorisierung  

---

## 🚀 Verwendungsbeispiele

### 1. Login durchführen

```bash
curl -X POST http://localhost:9090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@school.at",
    "password": "password123"
  }'
```

**Response**:
```json
{
  "success": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xsZSI6IlNDSFVFTEVSIiwianVzZXJJZCI6IjEyMzQ1In0.signature",
  "user": {
    "id": "12345",
    "name": "Max Mustermann",
    "email": "student@school.at",
    "className": "5AHIT",
    "role": "SCHUELER"
  }
}
```

### 2. Token validieren

```bash
curl -X GET http://localhost:9090/api/auth/validate \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 3. Geschützter Endpoint mit Token

```bash
curl -X GET http://localhost:9090/api/protected/resource \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 4. Fehlerfall: Ungültiger/Abgelaufener Token

```bash
curl -X GET http://localhost:9090/api/protected/resource \
  -H "Authorization: Bearer invalid-token"
```

**Response** (401):
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

---

## 📁 Dateistruktur

```
src/main/java/itsi/api/steuerung/
├── config/
│   ├── SecurityConfig.java          # Spring Security Konfiguration
│   └── JwtAuthenticationFilter.java  # JWT Token-Verarbeitung
├── controller/
│   └── AuthController.java           # Login/Validate Endpoints
├── service/
│   ├── JwtService.java               # JWT Token Management
│   └── AuthService.java              # Authentifizierung Logik
└── dto/
    ├── LoginRequest.java             # Login-Request DTO
    ├── LoginResponse.java            # Auth-Response DTO
    └── UserDTO.java                  # User-Daten DTO

src/main/resources/
└── application.properties             # JWT Konfiguration
```

---

## 📊 Abhängigkeiten

**Maven Dependencies** (aus `pom.xml` oder `build.gradle`):

```xml
<!-- JWT Processing -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Logging -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

---

## 🔧 Testing

### Unit Test - Login erfolgreich
```java
@Test
public void testLoginSuccess() {
    LoginRequest request = new LoginRequest("user@test.at", "password123");
    ResponseEntity<LoginResponse> response = authController.login(request);
    
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().isSuccess());
    assertNotNull(response.getBody().getToken());
}
```

### Unit Test - Token validieren
```java
@Test
public void testValidateToken() {
    String token = jwtService.generateToken(testUser);
    boolean isValid = jwtService.isTokenValid(token);
    
    assertTrue(isValid);
}
```

### Unit Test - Abgelaufener Token
```java
@Test
public void testExpiredToken() {
    // Simuliere abgelaufenen Token
    boolean isValid = jwtService.isTokenValid(expiredToken);
    
    assertFalse(isValid);
}
```

---

## 📝 Zusammenfassung

| Aspekt | Details |
|--------|---------|
| **Auth-Typ** | JWT (JSON Web Token) |
| **Algorithmus** | HS256 (HMAC-SHA256) |
| **Gültigkeitsdauer** | 24 Stunden |
| **Rollen** | SCHUELER, LEHRER, ADMIN |
| **Login Endpoint** | POST /api/auth/login |
| **Validate Endpoint** | GET /api/auth/validate |
| **Session-Typ** | Stateless (Keine Server-Sessions) |
| **CSRF Protection** | Deaktiviert (für API) |

---

## 🎯 Nächste Schritte (Empfehlungen)

1. ✅ **BCrypt für Passwörter implementieren** (KRITISCH)
2. ✅ **JWT Secret aus Umgebungsvariable laden** (KRITISCH)
3. ✅ **Wildcard-Autorisierung einschränken** (KRITISCH)
4. ✅ **Rate Limiting auf Login-Endpoint** (HOCH)
5. ✅ **Refresh Token Mechanismus** (MITTEL)
6. ✅ **Token Blacklist für Logout** (MITTEL)
7. ✅ **HTTPS erzwingen** (MITTEL)
8. ✅ **Passwort-Logging entfernen** (MITTEL)

---

**Dokumentation erstellt**: 2026-03-15  
**Backend Version**: Spring Boot 3.x  
**JWT Library**: jjwt 0.12.3  
