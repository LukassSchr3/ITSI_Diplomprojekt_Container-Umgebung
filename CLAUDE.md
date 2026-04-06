# ITSI Diplomprojekt – Container-Umgebung

## Projektübersicht

Lernplattform mit containerisierten Lernumgebungen (VNC/noVNC). Schüler starten Docker-Container über ein Web-Frontend und können darin Aufgaben lösen.

## Architektur

```
Frontend (React)
    ↓ REST + WebSocket
api/steuerung (Port 9090) – Spring Boot, Java 25
    ↓ WebClient (JWT wird automatisch weitergeleitet)
api/database (Port 5050) – Spring Boot, Java 25, PostgreSQL
    ↓ REST
backend (Port 3030) – Go, Container-Steuerung (start/stop/reset)
```

### Module

| Modul | Pfad | Port | Beschreibung |
|---|---|---|---|
| steuerung | `api/steuerung` | 9090 | Middleware: Auth, Container-Ops, Live-Environments |
| database | `api/database` | 5050 | CRUD-API für alle Entitäten |
| frontend | `frontend/` | - | React SPA |
| backend | `backend/` | 3030 | Go-Service, steuert Docker-Container direkt |

## Build & Start

```bash
# steuerung
cd api/steuerung
./gradlew build
./gradlew bootRun

# database
cd api/database
./gradlew build
./gradlew bootRun

# Tests
./gradlew test         # Unit-Tests
./gradlew qaTest       # ArchUnit + SpringBootTest
```

## Sicherheitskonzept (implementiert)

### JWT
- Rollen: `SCHUELER`, `LEHRER`, `ADMIN`
- Token-Claims: `userId`, `rolle`, `klasse`, `ablaufJahr`, `email`
- Algorithmus: HMAC-SHA256, Ablauf: 24h
- Secret: Umgebungsvariable `JWT_SECRET` (Fallback: Base64-Wert in application.properties)
- **Beide Module** (steuerung + database) validieren JWT

### JWT-Forwarding (steuerung → database)
- `WebClientConfig.jwtForwardingFilter()` leitet Authorization-Header automatisch weiter
- Keine manuelle Token-Weitergabe in Services nötig

### Rollenbasierter Zugriff (`@PreAuthorize`)

| Endpunkt-Typ | SCHUELER | LEHRER | ADMIN |
|---|---|---|---|
| Eigene Daten lesen | ✅ | ✅ | ✅ |
| Alle User/Instances lesen | ❌ | ✅ | ✅ |
| Kurse/Tasks/Fragen lesen | ✅ | ✅ | ✅ |
| Kurse/Tasks/Fragen erstellen | ❌ | ✅ | ✅ |
| User erstellen/löschen | ❌ | ❌ | ✅ |
| Images verwalten | ❌ | ❌ | ✅ |
| Live-Environment erstellen | ❌ | ✅ | ✅ |
| Container start/stop/reset | ✅ (eigener) | ✅ | ✅ |

Ownership-Checks über `@securityService.isOwner(#userId)` Bean in beiden Modulen.

### SecurityConfig (beide Module)
- CSRF disabled (stateless API)
- Öffentlich: `/api-docs/**`, `/swagger-ui/**`, `/api/info`, `/api/health`, `/api/auth/**`
- `@EnableMethodSecurity` aktiv

## Datenbankmodell (PostgreSQL, Port 5432)

**Entitäten:** `User`, `Image`, `Instance`, `LiveEnvironment`, `Course`, `Task`, `Question`, `QuestionResult`, `TaskGrade`, `StudentCourse`, `CourseTask`

**Verbindung:** `itsi_user` / `itsi_password` @ `itsi_db`

## Wichtige Konfigurationsdateien

| Datei | Zweck |
|---|---|
| `api/steuerung/src/main/resources/application.properties` | Ports, URLs, JWT |
| `api/database/src/main/resources/application.properties` | DB-Verbindung, JWT |
| `docker-compose.yml` | Gesamtes System als Container |
| `config/checkstyle/checkstyle.xml` | Checkstyle-Regeln (ignoreFailures=true) |

## Projektstruktur (steuerung)

```
config/          – SecurityConfig, JwtAuthenticationFilter, WebClientConfig, WebSocketConfig
controller/      – AuthController, ContainerController, LiveEnvironmentController
service/         – JwtService, SecurityService, AuthService, ContainerService, DatabaseService
dto/             – ContainerOperationRequest/Response, UserDTO
websocket/       – LiveEnvironmentWebSocketHandler
```

## Projektstruktur (database)

```
config/          – SecurityConfig, JwtAuthenticationFilter, CorsConfig
controller/      – 11 Controller für alle Entitäten
service/         – JwtService, SecurityService + ein Service pro Entität
entity/          – JPA-Entitäten
dto/             – UserDTO, CreateUserDTO, UpdateUserDTO, DashboardCourseDTO, EnrollClassDTO
mapper/          – UserMapper
repository/      – Spring Data JPA Repositories
```

## Bekannte Besonderheiten

- `api/steuerung/build.gradle` verwendet `Cedar Policy 3.3.0` (momentan ungenutzt)
- `steuerung` nutzt Spring WebFlux (`WebClient`) für ausgehende Requests, aber Spring MVC für eingehende
- `LiveEnvironment.vncPort` wird auto-berechnet: `max(existierender Port) + 1` oder `5900 + userId`
- WebSocket-Endpunkt in steuerung sendet Live-Environment-Status-Updates an das Frontend
- JaCoCo excludes: `config/`, `entity/`, `dto/`, `*Application*`
- Source Sets: `test/` für Unit-Tests, `qaTest/` für ArchUnit/SpringBootTest

## Gradle-Tasks

```bash
./gradlew test          # Unit-Tests mit JaCoCo
./gradlew qaTest        # QS/QM-Tests (ArchUnit, SpringBootTest)
./gradlew checkstyleMain
./gradlew jacocoTestReport
```

## Git

- Main Branch: `main`
- Aktueller Feature-Branch: `feat-fragen`
- Remote: GitHub (LukassSchr3/ITSI_Diplomprojekt_Container-Umgebung)
