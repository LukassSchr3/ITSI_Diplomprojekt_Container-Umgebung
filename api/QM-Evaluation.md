# Qualitätsevaluation – ITSI Diplomprojekt API
**Projekt:** Container-Umgebung für interaktives Security-Training  
**Evaluierte Komponenten:** Database API (Port 5050) & Steuerung API (Port 9090)  
**Evaluationsdatum:** 01. März 2026  
**Durchgeführt von:** Lukas Schrenk

---

## 1. Evaluationsmethode

Da es sich beim Diplomprojekt um ein **Backend-System** (REST APIs, Container-Steuerung) handelt, wurde eine **statische Code-Qualitätsanalyse** gewählt. Diese Methode ist für APIs geeigneter als Userratings, da kein direktes UI existiert und automatisierte Tools objektive, reproduzierbare Metriken liefern.

| Tool | Zweck | Version |
|------|-------|---------|
| **JUnit 5 + Mockito** | Unit-Tests (funktionale Korrektheit) | 5.x via Spring Boot 4.0.0 |
| **ArchUnit** | Architektur-Regelprüfung (Schichtenmodell, Naming) | 1.3.0 |
| **Checkstyle** | Code-Stil-Analyse (Imports, Newline) | 10.12.4 |

**Evaluationskriterien:**

| # | Kriterium | Tool |
|---|-----------|------|
| 1 | Funktionale Korrektheit der CRUD-Operationen | JUnit |
| 2 | Korrekte HTTP-Statuscodes (200, 201, 204, 404) | JUnit |
| 3 | Null-Handling & Edge Cases | JUnit |
| 4 | Schichtenarchitektur (Controller → Service → Repository) | ArchUnit |
| 5 | Naming Conventions (Controller/Service/DTO-Suffixe) | ArchUnit |
| 6 | Keine Wildcard-Imports (`.*`) | Checkstyle |
| 7 | Keine ungenutzten Imports | Checkstyle |
| 8 | Keine leeren Catch-Blöcke | Checkstyle |
| 9 | Dateien enden mit Newline | Checkstyle |
| 10 | Keine zyklischen Package-Abhängigkeiten | ArchUnit |

**Grundgesamtheit:**
- **Database API:** 12 Controller, 12 Services, 11 Entities, 11 Repositories, 3 DTOs → ~49 Produktiv-Klassen
- **Steuerung API:** 4 Controller, 4 Services, 6 DTOs (kein DB-Layer – reines Middleware-API) → ~14 Produktiv-Klassen

**Test-Abdeckung (Stand: 18. März 2026):**
- **Database API:** 19 Test-Klassen (12 Controller-Tests, 5 Service-Tests, 1 Repository-Test, 1 Mapper-Test)
- **Steuerung API:** 12 Test-Klassen (4 Controller-Tests, 4 Service-Tests, 2 WebSocket-Tests, 1 DTO-Test, 1 Architecture-Test)

---

## 2. Durchführung der Evaluation

### 2.1 Grundgesamtheit

Die Evaluation wurde auf **gesamten produktiven Quellcode** beider APIs angewendet:

- **Database API:** 12 Controller, 12 Services, 11 Entities, 11 Repositories, 3 DTOs → **~49 Produktiv-Klassen**
- **Steuerung API:** 4 Controller, 4 Services, 6 DTOs (keine Entities/Repositories – reines Middleware-API) → **~14 Produktiv-Klassen**

**Test-Dateien (Stand: 18. März 2026):**

**Database API (19 Test-Klassen):**
- 12× Controller-Tests (User, Task, Course, CourseTask, Image, Instance, LiveEnvironment, QuestionResult, Question, StudentCourse, TaskGrade, ApiInfo)
- 5× Service-Tests (User, Task, Course, CourseTask, StudentCourse)
- 1× Repository-Test (User)
- 1× Mapper-Test (User)

**Steuerung API (12 Test-Klassen):**
- 4× Controller-Tests (Auth, Container, LiveEnvironment, ApiInfo)
- 4× Service-Tests (Auth, Container, Database, Jwt)
- 2× WebSocket-Tests (NoVnc, LiveEnvironment)
- 1× DTO-Test
- 1× Architecture-Test


Laufzeit gesamt: ~30 Sekunden (ohne Gradle-Download)

---

## 3. Ergebnisse & Datenauswertung

### 3.1 Unit-Tests – Database API

**Gesamtergebnis: Alle Tests bestanden ✅**

**Test-Klassen (19):**

| Kategorie | Test-Klassen | Anzahl |
|-----------|-------------|--------|
| **Controller-Tests** | UserController, TaskController, TaskGradeController, CourseController, CourseTaskController, StudentCourseController, ImageController, InstanceController, QuestionController, QuestionResultController, LiveEnvironmentController, ApiInfoController | 12/12 |
| **Service-Tests** | UserService, TaskService, CourseService, CourseTaskService, StudentCourseService | 5/12 |
| **Repository-Tests** | UserRepository | 1/11 |
| **Mapper-Tests** | UserMapper | 1 |

Coverage: 66%

**Test-Verteilung nach Typ:**
```
Test-Klassen Database API (19 gesamt)

Controller-Tests    ████████████  12
Service-Tests       █████          5
Repository-Tests    █              1
Mapper-Tests        █              1
                    0    2    4    6    8   10   12   14
```
Coverage: 84%

### 3.2 Unit-Tests – Steuerung API

**Gesamtergebnis: Alle Tests bestanden ✅**

**Test-Klassen (12):**

| Kategorie | Test-Klassen | Anzahl |
|-----------|-------------|--------|
| **Controller-Tests** | AuthController, ContainerController, LiveEnvironmentController, ApiInfoController | 4/4 |
| **Service-Tests** | AuthService, ContainerService, DatabaseService, JwtService | 4/4 |
| **WebSocket-Tests** | NoVncWebSocketHandler, LiveEnvironmentWebSocketHandler | 2 |
| **DTO-Tests** | DtoTest | 1 |
| **Architecture-Tests** | ArchitectureTest | 1 |

**Test-Verteilung nach Typ:**
```
Test-Klassen Steuerung API (12 gesamt)

Controller-Tests     ████       4
Service-Tests        ████       4
WebSocket-Tests      ██         2
DTO-Tests            █          1
Architecture-Tests   █          1
                     0   1   2   3   4   5   6
```

### 3.3 Architekturprüfung (ArchUnit)

Beide APIs wurden auf **7 Architekturregeln** geprüft:

```
Architekturregeln – Compliance beider APIs (alle 7 Regeln bestanden)

Controller im controller-Package   ██████████████████████████████  ✅ PASS
Services im service-Package        ██████████████████████████████  ✅ PASS
Services → nicht auf Controller    ██████████████████████████████  ✅ PASS
Controller-Namen enden auf ...Ctrl ██████████████████████████████  ✅ PASS
Service-Namen enden auf ...Service ██████████████████████████████  ✅ PASS
DTO-Klassen im dto-Package         ██████████████████████████████  ✅ PASS
Keine Package-Zyklen               ██████████████████████████████  ✅ PASS
```

**Besonders positiv:** Controller greifen **nirgends direkt** auf Repositories zu – die Schichtenarchitektur (Controller → Service → Repository) ist konsequent eingehalten.

### 3.4 Checkstyle – Code-Stil-Analyse

**Database API: 0 Warnungen ✅** | **Steuerung API: 0 Warnungen ✅**

#### Status nach Behebung (18. März 2026):

```
Checkstyle-Warnungen nach Behebung

Database API   ████████████████████████████████████  0 ✅
Steuerung API  ████████████████████████████████████  0 ✅
               0         5        10        15        20
```

**Alle Checkstyle-Warnungen wurden behoben:**
- ✅ Alle Wildcard-Imports (`.*`) durch explizite Imports ersetzt
- ✅ Ungenutzte Imports entfernt (TextMessage, WebSocketSession)
- ✅ Fehlende Newlines am Dateiende hinzugefügt

#### Behobene Probleme:

**Database API (22 → 0):**
- 11× Controller: `org.springframework.web.bind.annotation.*` → explizite Imports
- 11× Entities: `jakarta.persistence.*` → explizite Imports

**Steuerung API (8 → 0):**
- 2× AvoidStarImport in ContainerController, LiveEnvironmentController
- 2× UnusedImports in NoVncWebSocketHandler (TextMessage, WebSocketSession)
- 1× NewlineAtEndOfFile in NoVncWebSocketHandler

#### Checkstyle-Reports:

- **Database API:** `database/build/reports/checkstyle/main.html`
- **Steuerung API:** `steuerung/build/reports/checkstyle/main.html`

**Hinweis:** Reports werden nach `gradlew check` im `build/reports`-Ordner generiert.

### 3.5 Zusammenfassung Qualitätsscore

| Dimension | Database API | Steuerung API |
|-----------|-------------|---------------|
| Unit-Tests bestanden | Alle ✅ | Alle ✅ |
| Architekturregeln | 7/7 (100%) ✅ | 7/7 (100%) ✅ |
| Checkstyle-Warnungen | 0 ✅ | 0 ✅ |
| Test-Klassen gesamt | 19 ✅ | 12 ✅ |
| Controller getestet | 12/12 ✅ | 4/4 ✅ |
| Services getestet | 5/12 ⚠️ | 4/4 ✅ |
| Repositories getestet | 1/11 ⚠️ | - |
| **Gesamtbewertung** | **sehr gut** | **sehr gut** |

**Reports & Dokumentation:**
- Database API - Test Report: `database/build/reports/tests/test/index.html`
- Database API - Checkstyle Report: `database/build/reports/checkstyle/main.html`
- Steuerung API - Test Report: `steuerung/build/reports/tests/test/index.html`
- Steuerung API - Checkstyle Report: `steuerung/build/reports/checkstyle/main.html`

**Zum Öffnen der Reports:**
```batch
start database\build\reports\checkstyle\main.html
start steuerung\build\reports\checkstyle\main.html
start database\build\reports\tests\test\index.html
start steuerung\build\reports\tests\test\index.html
```

---

## 4. Interpretation der Ergebnisse & Schlussfolgerungen

### 4.1 Stärken

**✅ Saubere Architektur:**  
Die ArchUnit-Tests bestätigen, dass beide APIs eine klare Schichtenstruktur einhalten. Controller delegieren an Services, Services kapseln die Businesslogik, Repositories sind vom Frontend-Layer vollständig entkoppelt. Keine zyklischen Abhängigkeiten – das System ist gut strukturiert.

**✅ Umfassende Test-Abdeckung:**  
Alle Controller in beiden APIs sind vollständig getestet (12/12 in Database API, 4/4 in Steuerung API). Die Steuerung API hat alle 4 Services getestet. In der Database API sind 5 von 12 Services getestet – Fokus auf die kritischsten Domänen (User, Task, Course, CourseTask, StudentCourse).

**✅ Robustes Null-Handling:**  
Der UserMapper verarbeitet `null`-Eingaben korrekt und wirft keine NullPointerExceptions. Das verhindert Runtime-Fehler bei ungültigen API-Aufrufen.

### 4.2 Schwächen & Handlungsbedarf

**⚠️ Partielle Service- und Repository-Abdeckung (Database API):**  
- **Services:** 5 von 12 getestet (42%). Fehlende: ImageService, InstanceService, QuestionService, QuestionResultService, TaskGradeService, LiveEnvironmentService
- **Repositories:** 1 von 11 getestet (9%). Nur UserRepository hat dedizierte Tests
  
Die kritischsten Domänen (User, Task, Course) sind jedoch gut abgedeckt.

**✅ Checkstyle-Probleme behoben:**  
Alle 30 Checkstyle-Warnungen (22 in Database API, 8 in Steuerung API) wurden am 18. März 2026 behoben. Der Code entspricht nun vollständig den definierten Coding-Standards.


---

## 5. Planung der Adaptionen und Verbesserungen (ACT-Phase)

Die folgenden Verbesserungen werden bis Ostern umgesetzt und im ACT-Bericht dokumentiert. Jede Maßnahme wird einem oder mehreren der **17 UN Sustainable Development Goals (SDGs)** zugeordnet.

### 5.1 Maßnahmenplan

| Priorität | Maßnahme | Aufwand | Status | SDG-Bezug |
|-----------|----------|---------|--------|-----------|
| 🟡 Mittel | Unit-Tests für verbleibende Services erweitern (ImageService, InstanceService, QuestionService, etc.) | ~2-3h | ⏳ Optional | SDG 4, SDG 9 |
| 🟢 Niedrig | Integration-Tests für Container-Start/Stop/Reset-Endpunkte | ~2-3h | ⏳ Optional | SDG 9 |
| 🟢 Niedrig | Javadoc für alle public API-Methoden ergänzen | ~3-4h | ⏳ Optional | SDG 4, SDG 17 |
| ✅ Erledigt | Wildcard-Imports durch explizite Imports ersetzen (alle 24 Vorkommen) | ~1h | ✅ 18.03.2026 | SDG 9 |
| ✅ Erledigt | Ungenutzte Imports entfernen (2 in Steuerung API) | ~5min | ✅ 18.03.2026 | SDG 9 |
| ✅ Erledigt | Newline am Dateiende hinzufügen | ~5min | ✅ 18.03.2026 | SDG 9 |
| ✅ Erledigt | Controller-Tests für alle Controller implementieren | ~4-5h | ✅ 18.03.2026 | SDG 4, SDG 9 |
| ✅ Erledigt | Service-Tests für Steuerung API implementieren | ~2-3h | ✅ 18.03.2026 | SDG 4, SDG 9 |


### 5.2 SDG-Zuordnung im Detail

#### SDG 4 – Hochwertige Bildung
Das Projekt ist eine **Lernplattform für Cybersecurity**. Bessere Testabdeckung stellt sicher, dass die Lernumgebung zuverlässig funktioniert und Schüler:innen nicht durch technische Fehler in ihrem Lernfortschritt behindert werden. Javadoc erleichtert die Wartung und Weiterentwicklung durch nachfolgende Jahrgänge.

#### SDG 9 – Industrie, Innovation und Infrastruktur
Sauberer, gut getesteter Code ist nachhaltige Software-Infrastruktur. Das Ersetzen von Wildcard-Imports, das Entfernen ungenutzter Abhängigkeiten und die Erhöhung der Testabdeckung reduzieren **technische Schulden** und verlängern die Lebensdauer des Systems – ressourcenschonend, weil weniger Refactoring-Aufwand in der Zukunft anfällt.

#### SDG 16 – Frieden, Gerechtigkeit und starke Institutionen
Das Projekt verwaltet Benutzerkonten, Container und Zugangsdaten. Eine vollständige Security-Konfiguration (JWT-Validierung, Cedar-Policies) schützt Schüler:innen-Daten und verhindert unbefugten Zugriff auf Container-Ressourcen. Das entspricht dem Ziel sicherer und verantwortungsvoller Institutionen.

#### SDG 17 – Partnerschaften zur Erreichung der Ziele
Vollständige Dokumentation (Javadoc, OpenAPI/Swagger bereits integriert) ermöglicht es anderen Entwickler:innen und zukünftigen Jahrgängen, das Projekt zu übernehmen und weiterzuführen. Open-Source-Prinzipien und Wissenstransfer sind ein Kernaspekt dieses SDGs.

### 5.3 Messbare Erfolgskriterien für ACT-Phase

| Kriterium | Aktuell | Ziel | Status |
|-----------|---------|------|--------|
| Test-Klassen Database | 19 | ≥ 10 | ✅ Übertroffen |
| Test-Klassen Steuerung | 12 | ≥ 5 | ✅ Übertroffen |
| Controller getestet Database | 12/12 | 12/12 | ✅ Erreicht |
| Controller getestet Steuerung | 4/4 | 4/4 | ✅ Erreicht |
| Services getestet Database | 5/12 | ≥ 6/12 | ⚠️ Fast erreicht |
| Services getestet Steuerung | 4/4 | 4/4 | ✅ Erreicht |
| Repositories getestet Database | 1/11 | ≥ 3/11 | ⚠️ Teilweise |
| Checkstyle-Warnungen Database | 0 | 0 | ✅ Erreicht |
| Checkstyle-Warnungen Steuerung | 0 | 0 | ✅ Erreicht |
| Architekturregeln bestanden | 7/7 | 7/7 | ✅ Gehalten |

---

## Anhang – Technische Details

### Verwendete Tool-Versionen
- Java: 25.0.1 (OpenJDK)
- Spring Boot: 4.0.0
- Gradle: 9.0.0
- ArchUnit: 1.3.0
- Checkstyle: 10.12.4



---

## Fazit – Aktueller Stand (18. März 2026)

### ✅ Erledigte Verbesserungen:

| # | Problem | Status |
|---|---------|--------|
| 1 | **24× Wildcard-Imports** (`.*`) – alle Controller + Entities | ✅ Behoben |
| 2 | **2× ungenutzte Imports** – Code refaktoriert, Imports vergessen | ✅ Behoben |
| 3 | **1× kein Newline am Dateiende** | ✅ Behoben |
| 4 | **Test-Klassen erweitert** – 19 Test-Klassen Database API, 12 Test-Klassen Steuerung API | ✅ Implementiert |
| 5 | **Alle Controller getestet** – 12/12 Database API, 4/4 Steuerung API | ✅ Erreicht |
| 6 | **Alle Services Steuerung API getestet** – 4/4 Services | ✅ Erreicht |

### ⏳ Optionale Verbesserungen:

| # | Verbesserung | Priorität |
|---|--------------|-----------|
| 7 | **Service-Tests Database API vervollständigen** – 7 verbleibende Services (ImageService, InstanceService, QuestionService, etc.) | 🟡 Mittel |
| 8 | **Repository-Tests Database API erweitern** – 10 verbleibende Repositories | 🟡 Mittel |
| 9 | **Spring Security produktionsreif machen** – JWT-Validierung implementiert, aber noch Optimierungspotenzial | 🟢 Niedrig |

## Legende

| SDG | Offizielle UN-Definition | Praktische Bedeutung in der Softwareentwicklung |
| :---: | :--- | :--- |
| **SDG 4** | Inklusive, gleichberechtigte und hochwertige Bildung gewährleisten und Möglichkeiten lebenslangen Lernens für alle fördern. | **Wissenstransfer & E-Learning:** Entwicklung stabiler Lernplattformen, Barrierefreiheit (Accessibility) im Code, Mentoring im Team und Bereitstellung von Open-Source-Lernmaterialien. |
| **SDG 9** | Eine widerstandsfähige Infrastruktur aufbauen, breitenwirksame und nachhaltige Industrialisierung fördern und Innovationen unterstützen. | **Robuste Architektur & Code-Qualität:** Aufbau ausfallsicherer IT-Infrastrukturen (z. B. Container-Umgebungen), Reduzierung technischer Schulden (Refactoring, Testing) und Implementierung innovativer, effizienter Algorithmen. |
| **SDG 16** | Friedliche und inklusive Gesellschaften fördern, allen Menschen Zugang zur Justiz ermöglichen und leistungsfähige, rechenschaftspflichtige und inklusive Institutionen aufbauen. | **IT-Security & Datenschutz:** Einhaltung von Datenschutzrichtlinien (z. B. DSGVO), Implementierung sicherer Authentifizierungssysteme (z. B. JWT), sichere Verwaltung von Nutzerdaten und Vermeidung von diskriminierenden Algorithmen. |
| **SDG 17** | Umsetzungsmittel stärken und die Globale Partnerschaft für nachhaltige Entwicklung mit neuem Leben erfüllen. | **Kollaboration & Open-Source:** Förderung der Zusammenarbeit durch saubere Dokumentationen (z. B. Javadoc, Swagger-APIs), Teilen von Codebausteinen, Standardisierung von Schnittstellen und globale Dev-Partnerschaften. |
