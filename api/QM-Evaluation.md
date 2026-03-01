# Qualitätsevaluation – ITSI Diplomprojekt API
**Projekt:** Container-Umgebung für interaktives Security-Training  
**Evaluierte Komponenten:** Database API (Port 5050) & Steuerung API (Port 9090)  
**Evaluationsdatum:** 01. März 2026  
**Durchgeführt von:** Projektgruppe ITSI, 5. Jahrgang TGM  

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
- **Steuerung API:** 3 Controller, 4 Services, 6 DTOs (kein DB-Layer – reines Middleware-API) → ~13 Produktiv-Klassen

Unit-Tests wurden für die **User-Domäne** als repräsentative Stichprobe implementiert.

---

## 2. Durchführung der Evaluation

### 2.1 Grundgesamtheit

Die Evaluation wurde auf **gesamten produktiven Quellcode** beider APIs angewendet:

- **Database API:** 12 Controller, 12 Services, 11 Entities, 11 Repositories, 3 DTOs → **~49 Produktiv-Klassen**
- **Steuerung API:** 3 Controller, 4 Services, 6 DTOs (keine Entities/Repositories – reines Middleware-API) → **~13 Produktiv-Klassen**

Unit-Tests wurden für die **User-Domäne** als repräsentative Stichprobe implementiert (Controller, Service, Repository, Mapper).

### 2.2 Ausführung

```
quality-check.bat   →   läuft automatisiert für beide APIs
                    →   Reports in C:\tmp\itsi-build\{database|steuerung}\reports\
```

Laufzeit gesamt: ~30 Sekunden (ohne Gradle-Download)

---

## 3. Ergebnisse & Datenauswertung

### 3.1 Unit-Tests – Database API

**Gesamtergebnis: 46 Tests, 0 Fehlgeschlagen ✅**

| Testklasse | Tests | Bestanden | Fehlgeschlagen | Laufzeit |
|------------|-------|-----------|----------------|----------|
| UserControllerTest | 13 | 13 | 0 | 189 ms |
| UserServiceTest | 12 | 12 | 0 | 15 ms |
| UserRepositoryTest | 11 | 11 | 0 | 116 ms |
| UserMapperTest | 9 | 9 | 0 | 7 ms |
| DatabaseApplicationTests | 1 | 1 | 0 | 161 ms |
| ArchitectureTest | 7 | 7 | 0 | ~800 ms |
| **Gesamt** | **53** | **53** | **0** | **~1,3 s** |

```
Unit-Tests Database API – Ergebnisse nach Testklasse
(Anzahl bestandener Tests)

UserControllerTest  ████████████████████████████████  13
UserServiceTest     ████████████████████████████████  12  (skaliert)
UserRepositoryTest  ████████████████████████████████  11
UserMapperTest      ████████████████████████████████   9
ArchitectureTest    ████████████████████████████████   7
DatabaseAppTests    ████                               1
                    0    2    4    6    8   10   12   14
```

### 3.2 Unit-Tests – Steuerung API

**Gesamtergebnis: 8 Tests, 0 Fehlgeschlagen ✅**

| Testklasse | Tests | Bestanden | Fehlgeschlagen |
|------------|-------|-----------|----------------|
| SteuerungApplicationTests | 1 | 1 | 0 |
| ArchitectureTest | 7 | 7 | 0 |
| **Gesamt** | **8** | **8** | **0** |

> **Hinweis:** Die Steuerung API hat deutlich weniger Unit-Tests als die Database API. Das ist ein identifiziertes Verbesserungspotenzial (siehe Kapitel 5).

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

**Database API: 22 Warnungen** | **Steuerung API: 8 Warnungen**

#### Database API – Checkstyle Warnungen nach Kategorie:

```
Checkstyle-Warnungen Database API (gesamt: 22)

AvoidStarImport (Controller)  ███████████████████████████  11
AvoidStarImport (Entities)    ███████████████████████████  11
Sonstige                                                    0
                              0    2    4    6    8   10   12
```

Alle 22 Warnungen sind vom Typ **AvoidStarImport**:
- 11× Controller verwenden `org.springframework.web.bind.annotation.*`
- 11× Entities verwenden `jakarta.persistence.*`

#### Steuerung API – Checkstyle Warnungen nach Kategorie:

```
Checkstyle-Warnungen Steuerung API (gesamt: 8)

AvoidStarImport               ████████████████              2
UnusedImports                 ████████████████████████      4
NewlineAtEndOfFile            ████████                      2
                              0    1    2    3    4    5
```

| Kategorie | Anzahl | Betroffene Dateien |
|-----------|--------|-------------------|
| AvoidStarImport | 2 | ContainerController.java, LiveEnvironmentController.java |
| UnusedImports | 4 | DatabaseService.java (Duration), NoVncWebSocketHandler.java (TextMessage, WebSocketSession), ContainerController.java (ImageDTO) |
| NewlineAtEndOfFile | 2 | 2 Dateien ohne abschließenden Zeilenumbruch |

#### Vergleich beider APIs:

```
Checkstyle-Warnungen gesamt im Vergleich

Database API   ██████████████████████████████████████████████  22
Steuerung API  ████████████████████                             8
               0         5        10        15        20       25
```

### 3.5 Zusammenfassung Qualitätsscore

| Dimension | Database API | Steuerung API |
|-----------|-------------|---------------|
| Unit-Tests bestanden | 53/53 (100%) ✅ | 8/8 (100%) ✅ |
| Architekturregeln | 7/7 (100%) ✅ | 7/7 (100%) ✅ |
| Checkstyle-Warnungen | 22 ⚠️ | 8 ⚠️ |
| Test-Abdeckung (Klassen) | ~8% (User-Domäne) ⚠️ | ~10% ⚠️ |
| **Gesamtbewertung** | **gut** | **gut** |

---

## 4. Interpretation der Ergebnisse & Schlussfolgerungen

### 4.1 Stärken

**✅ Saubere Architektur:**  
Die ArchUnit-Tests bestätigen, dass beide APIs eine klare Schichtenstruktur einhalten. Controller delegieren an Services, Services kapseln die Businesslogik, Repositories sind vom Frontend-Layer vollständig entkoppelt. Keine zyklischen Abhängigkeiten – das System ist gut strukturiert.

**✅ Korrekte Fehlerbehandlung:**  
Die UserController-Tests zeigen, dass HTTP-404 bei nicht gefundenen Ressourcen korrekt zurückgegeben wird, HTTP-201 bei Erstellung und HTTP-204 bei Löschung. Das entspricht REST-Best-Practices.

**✅ Robustes Null-Handling:**  
Der UserMapper verarbeitet `null`-Eingaben korrekt und wirft keine NullPointerExceptions. Das verhindert Runtime-Fehler bei ungültigen API-Aufrufen.

### 4.2 Schwächen & Handlungsbedarf

**⚠️ Wildcard-Imports (22+2 Vorkommen):**  
Alle Controller und Entities verwenden `import X.*` statt expliziter Imports. Das erhöht Compile-Zeit, macht Abhängigkeiten intransparent und kann zu Konflikten bei gleichnamigen Klassen führen.

**⚠️ Geringe Test-Abdeckung:**  
Nur die User-Domäne ist getestet. Es existieren **12 Services** in der Database API, aber Tests nur für einen (`UserService`). Die Steuerung API hat keine Service-Unit-Tests. Bei einem System das Container in einer Live-Umgebung steuert ist das ein Risiko.

**⚠️ Ungenutzte Imports in Steuerung:**  
4 ungenutzte Imports deuten auf refaktorisierten Code hin, bei dem Importe nicht bereinigt wurden. Indikator für technische Schulden.

**⚠️ Spring Security ohne vollständige Konfiguration:**  
Im Test-Log sichtbar: `Using generated security password` – die Security-Konfiguration ist für Produktion noch nicht finalisiert.

---

## 5. Planung der Adaptionen und Verbesserungen (ACT-Phase)

Die folgenden Verbesserungen werden bis Ostern umgesetzt und im ACT-Bericht dokumentiert. Jede Maßnahme wird einem oder mehreren der **17 UN Sustainable Development Goals (SDGs)** zugeordnet.

### 5.1 Maßnahmenplan

| Priorität | Maßnahme | Aufwand | SDG-Bezug |
|-----------|----------|---------|-----------|
| 🔴 Hoch | Unit-Tests für alle Services erweitern (mind. CourseService, TaskService, InstanceService, ContainerService) | ~3-4h | SDG 4, SDG 9 |
| 🔴 Hoch | Integration-Tests für Container-Start/Stop/Reset-Endpunkte | ~2-3h | SDG 9 |
| 🟡 Mittel | Wildcard-Imports durch explizite Imports ersetzen (alle 24 Vorkommen) | ~1h | SDG 9 |
| 🟡 Mittel | Ungenutzte Imports entfernen (4 in Steuerung API) | ~15min | SDG 9 |
| 🟡 Mittel | Newline am Dateiende hinzufügen | ~5min | SDG 9 |
| 🟢 Niedrig | Javadoc für alle public API-Methoden ergänzen | ~3-4h | SDG 4, SDG 17 |
| 🟢 Niedrig | Spring Security Produktionskonfiguration abschließen | ~2h | SDG 16 |

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

| Kriterium | Aktuell | Ziel |
|-----------|---------|------|
| Unit-Test-Klassen | 4 (Database) + 1 (Steuerung) | ≥ 10 (Database) + ≥ 5 (Steuerung) |
| Checkstyle-Warnungen Database | 22 | 0 |
| Checkstyle-Warnungen Steuerung | 8 | 0 |
| Architekturregeln bestanden | 7/7 | 7/7 (halten) |

---

## Anhang – Technische Details

### Verwendete Tool-Versionen
- Java: 25.0.1 (OpenJDK)
- Spring Boot: 4.0.0
- Gradle: 9.0.0
- ArchUnit: 1.3.0
- Checkstyle: 10.12.4

### Report-Pfade (lokal)
```
C:\tmp\itsi-build\database\reports\tests\test\index.html      ← JUnit + ArchUnit
C:\tmp\itsi-build\database\reports\checkstyle\main.html       ← Checkstyle
C:\tmp\itsi-build\steuerung\reports\tests\test\index.html     ← JUnit + ArchUnit
C:\tmp\itsi-build\steuerung\reports\checkstyle\main.html      ← Checkstyle
```

### Ausführen der Evaluation
```bat
api\quality-check.bat
```

---

## Fazit – Was muss geändert werden?

| # | Problem | Wo | Priorität |
|---|---------|-----|-----------|
| 1 | **24× Wildcard-Imports** (`.*`) – alle Controller + Entities | Database API (22), Steuerung API (2) | 🟡 Mittel |
| 2 | **4× ungenutzte Imports** – Code refaktoriert, Imports vergessen | Steuerung API | 🟡 Mittel |
| 3 | **2× kein Newline am Dateiende** | Steuerung API | 🟡 Mittel |
| 4 | **Geringe Testabdeckung** – nur User-Domäne getestet, 11 Services ohne Tests | Database API, Steuerung API | 🔴 Hoch |
| 5 | **Spring Security nicht produktionsreif** – generiertes Passwort statt echter Konfiguration | Steuerung API | 🔴 Hoch |
