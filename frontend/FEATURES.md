# ITSI Container Frontend - Ausführliche Feature-Dokumentation

> **Stand:** März 2026  
> **Framework:** Angular 21 (Signals, Standalone Components)  
> **HTTP-Client:** Axios  
> **Styling:** CSS (komponentenbasiert)  
> **VNC:** noVNC 1.3.0  
> **State Management:** Angular Signals + RxJS Observables

---

## Inhaltsverzeichnis

1. [Überblick & Architektur](#überblick--architektur)
2. [Authentifizierung & Autorisierung](#authentifizierung--autorisierung)
3. [Dashboard & Kursverwaltung](#dashboard--kursverwaltung)
4. [Quiz- & Aufgabensystem](#quiz---aufgabensystem)
5. [VNC Remote-Desktop Integration](#vnc-remote-desktop-integration)
6. [Admin-Panel](#admin-panel)
7. [Routing & Navigation](#routing--navigation)
8. [Service-Architektur](#service-architektur)
9. [Zustandsverwaltung](#zustandsverwaltung)
10. [HTTP-Kommunikation & APIs](#http-kommunikation--apis)
11. [Komponenten-Übersicht](#komponenten-übersicht)
12. [Datenmodelle](#datenmodelle)
13. [TypeScript & Konfiguration](#typescript--konfiguration)
14. [Docker & Deployment](#docker--deployment)
15. [Bekannte Einschränkungen](#bekannte-einschränkungen)

---

## Überblick & Architektur

Dies ist eine moderne **Angular 21 Frontend-Anwendung** für ein Container-basiertes Lernmanagementsystem (ITSI). Sie bietet Funktionalität für Schüler, Lehrer und Administratoren zur Verwaltung von Kursen, Aufgaben, Quiz und virtuellen Laborumgebungen mit VNC-Fernzugriff.

### Kern-Features
✅ JWT-basierte Authentifizierung mit Auto-Logout  
✅ Rollenbasierte Zugriffskontrolle (RBAC)  
✅ Interaktives Quiz-System mit Sofortfeedback  
✅ VNC Remote-Desktop Integration (noVNC 1.3.0)  
✅ Umfassendes Admin-Panel mit 7 CRUD-Tabs  
✅ Docker-Container Lifecycle-Management  
✅ Reaktive Architektur mit Angular Signals & RxJS  
✅ Parallele Datenladeung (Promise.all)  
✅ TypeScript Strict Mode

### Technologie-Stack
- **Framework:** Angular 21 (Signals, Standalone Components)
- **Sprache:** TypeScript 5.9.2 (Strict Mode)
- **HTTP:** Axios mit JWT-Interceptor
- **State:** Angular Signals + RxJS Observables
- **VNC:** noVNC 1.3.0
- **Deployment:** Docker + Nginx

---

## Authentifizierung & Autorisierung

### JWT-basierte Authentifizierung

**Service:** `src/app/service/auth.service.ts`

Die Authentifizierung erfolgt über JWT-Tokens mit automatischer Verwaltung:

**Login-Flow:**
1. LoginComponent zeigt Email/Passwort-Form
2. `AuthService.login()` sendet `POST http://localhost:9090/api/auth/login`
3. Backend antwortet mit JWT-Token
4. Token wird im `sessionStorage` (Key: `auth_token`) gespeichert
5. JWT-Payload wird dekodiert (Base64url)
6. `token`, `userId`, `roles` Signals werden aktualisiert
7. Router navigiert zu `/dashboard`

**Unterstützte JWT-Claims:**
- `sub` - Subject (Benutzer-ID)
- `email` - Benutzer E-Mail
- `userId` - Benutzer-Identität
- `roles` / `role` / `rolle` - Rollen-Array oder String
- `exp` - Ablaufzeitpunkt

**Token-Speicherung:**
- Speichert im `sessionStorage` (nicht `localStorage`)
- Wird automatisch beim Tab-Schließen gelöscht
- Länger Gültig: ca. 24 Stunden (Backend-abhängig)

### Auto-Logout bei Ablauf

- AuthService prüft alle **30 Sekunden** den `exp`-Claim des JWT-Tokens
- Bei Ablauf: Automatischer Logout und Weiterleitung zu `/login`
- Verhindert unbefugte Zugriffe mit abgelaufenen Tokens
- Benutzer wird informiert, dass Sitzung abgelaufen ist

### Rollenbasierte Zugriffskontrolle

**Drei Rollen werden unterstützt:**

| Rolle | Deutsch | Berechtigungen |
|-------|---------|---|
| `SCHUELER` | Schüler | Dashboard sehen, Kurse einsehen, Aufgaben/Quiz bearbeiten, VNC-Umgebungen öffnen |
| `LEHRER` | Lehrer | Alle Schüler-Berechtigungen + Schülerfortschritt benoten (`canGrade`), Änderungen sehen |
| `ADMIN` | Administrator | Vollzugriff auf alle Funktionen, Admin-Panel, alle CRUD-Operationen |

**Rollen-basierte Signale:**
```typescript
isAdmin()      // Computed Signal: true wenn Rolle ADMIN
isTeacher()    // Computed Signal: true wenn Rolle LEHRER  
isStudent()    // Computed Signal: true wenn Rolle SCHUELER
```

### Permission Service

**Datei:** `src/app/service/permission.service.ts`

Bietet granulare Berechtigungsprüfungen als berechnete Signale:

```typescript
canRead()               // true wenn Benutzer angemeldet
canWrite()              // true wenn Benutzer ADMIN
canGrade()              // true wenn LEHRER oder ADMIN
canManageContainer()    // true wenn ADMIN oder Besitzer des Containers
```

Diese werden in Komponenten verwendet um UI-Elemente bedingt anzuzeigen.

### AuthGuard

**Datei:** `src/app/guards/auth.guard.ts`

Protektionsmechanismus für alle gesicherten Routes:

```typescript
if (authService.isLoggedIn()() && !authService.isTokenExpired()) {
  return true;  // Zugriff erlaubt
}
// Token abgelaufen oder nicht angemeldet
authService.logout();
return false;  // Redirect zu /login
```

**⚠️ Sicherheitshinweis:** Die `/admin`-Route prüft nur Authentifizierung, nicht die Admin-Rolle. Schüler könnten URL direkt aufrufen. Sollte behoben werden.

---

## Dashboard & Kursverwaltung

### Personalisierte Kursansicht

**Komponente:** `src/app/components/dashboard/dashboard.component.ts`

Das Dashboard ist die Hauptseite nach dem Login und zeigt:

**Angezeigt für jeden Benutzer:**
- Alle eingeschriebenen Kurse des Benutzers
- Für jeden Kurs:
  - Kursname und Beschreibung
  - Einschreibungsdatum
  - Ablaufdatum (falls vorhanden)
  - Zugeordnete Aufgaben/Quizze

**Datenladung (Parallel via Promise.all):**
```
1. GET /api/student-courses/user/{userId} 
   → Liste eingeschriebener Kurse
   
2. Für jeden Kurs parallel:
   - GET /api/courses/{courseId} 
     → Kursdetails (Name, Beschreibung)
   - GET /api/course-tasks/course/{courseId}/tasks
     → Aufgabenliste für diesen Kurs
```

Paralleles Laden ermöglicht schnelle Seitenerstellung.

### Rollenbasierte UI

| UI-Element | SCHUELER | LEHRER | ADMIN |
|-----------|----------|--------|-------|
| Dashboard sehen | ✓ | ✓ | ✓ |
| Aufgaben starten | ✓ | ✓ | ✓ |
| Admin Panel Button | — | — | ✓ |
| Grade/Benoten Button | — | ✓ | ✓ |
| Status ändern Button | — | — | ✓ |
| Rollen-Label | ✓ | ✓ | ✓ |

### Statistik-Anzeige

Computed Signal zählt Übungen (aus ExerciseService) nach Status:

```typescript
stats = computed(() => {
  const exercises = this.exerciseService.getExercises()();
  return {
    notStarted: exercises.filter(e => e.status === 'not-started').length,
    inProgress: exercises.filter(e => e.status === 'in-progress').length,
    completed: exercises.filter(e => e.status === 'completed').length
  };
});
```

Gibt Schülern schnelle Übersicht über ihren Fortschritt.

### Navigation

- Klick auf Aufgabe → `/task/{taskId}`
- Klick auf "VNC" → `/image/{imageId}`
- Admin Button (nur Admins) → `/admin`
- Logout → `/login`

---

## Quiz- & Aufgabensystem

### Schritt-für-Schritt Quiz-Navigation

**Komponente:** `src/app/components/task-detail/task-detail.ts`

Die TaskDetail-Komponente bietet ein strukturiertes Quiz-System:

**Daten-Laden:**
```
1. Route-Parameter :taskId lesen
2. GET /api/tasks/{taskId} 
   → Aufgaben-Metadaten (title, description, points, imageId)
3. GET /api/questions/task/{taskId}
   → Alle Fragen für diese Aufgabe laden
```

**Quiz-Navigation:**
- Eine Frage pro Schritt
- Fortschrittsbalken zeigt prozentuale Fertigstellung
- "Zurück" Button kehrt zum Dashboard
- "Weiter" Button (nur bei richtiger Antwort aktiv)

### Antwortformat-Unterstützung

Antworten können in zwei Formaten gespeichert sein:

**Format 1: JSON-String (von API)**
```json
"[{\"text\":\"Option A\", \"richtig\":true, \"punkte\":10}, ...]"
```

**Format 2: Array**
```typescript
[{text: "Option A", richtig: true, punkte: 10}, ...]
```

Das System verarbeitet automatisch beide Formate via `parsedAnswers` Computed Signal.

### Sofortfeedback-System

Nach Beantwortung einer Frage:

1. **Antwort korrekt:**
   - ✓ "Richtig!" Meldung
   - "Weiter"-Button wird aktiv
   - Allows zum nächsten Quiz-Schritt

2. **Antwort falsch:**
   - ✗ "Falsch" Meldung
   - "Nochmal Versuchen"-Button wird aktiv
   - Erlaubt Retry ohne Strafpunkte
   - Auswahl wird zurückgesetzt

### Quiz-Abschluss

- Computed Signal `allAnswered` erkennt Quiz-Completion
- Beim Abschluss aller Fragen: Erfolgs-Meldung
- Button "Zur VNC-Umgebung gehen" ermöglicht Navigation
- Quiz-Daten bleiben für Nachbereitung verfügbar

---

## VNC Remote-Desktop Integration

### Verbindungsaufbau

**Services:** 
- `src/app/service/vnc.service.ts` - Hauptservice
- `src/app/service/novnc.ts` - Factory-Alternative

**Komponente:** `src/app/components/image/imageComponent.ts`

Der ImageComponent verwaltet die VNC-Verbindung:

**Verbindungs-Setup-Workflow:**
```
1. Route-Parameter (imageId) lesen
2. GET /api/live-environment/vnc-port/{userId}
   → VNC-Port und -Passwort abrufen
3. WebSocket-URL konstruieren: 
   ws://localhost:9090/ws/novnc?vncPort={port}
4. VncService.connect() aufrufen
5. noVNC RFB-Verbindung wird aufgebaut
6. Canvas wird in DOM-Element #vncScreen gerendert
7. Status-Observable abonnieren
```

### VNC-Verbindungs-Status

VncService bietet Observable `status$` mit 4 Status-Zuständen:

| Status | Deutsch | Bedeutung |
|--------|---------|-----------|
| `Getrennt` | DISCONNECTED | Nicht verbunden oder getrennt |
| `Verbindung wird hergestellt...` | CONNECTING | Verbindungsversuch läuft |
| `Verbunden` | CONNECTED | Aktive Remote-Desktop-Sitzung |
| `Fehler` | ERROR | Verbindungsfehler aufgetreten |

Die Komponente zeigt:
- Loading-Spinner während "Verbindung wird hergestellt..."
- Remote-Desktop Canvas bei "Verbunden"
- Fehlermeldung bei "Fehler"

### Authentifizierung

- VNC-Passwort wird automatisch beim `credentialsrequired`-Event gesendet
- Keine manuelle Passwort-Eingabe erforderlich
- Password stammt aus `GET /api/live-environment/vnc-port/{userId}`

### Remote Desktop Features

**Unterstützte Funktionen:**
- **Viewport-Skalierung** - Automatische Fenster-Anpassung auf Browser-Größe
- **Session-Resizing** - Größe des Remote-Desktops an Browser anpassen
- **Tastatureingaben** - Vollständige Keyboard-Unterstützung
  - Normale Tasten
  - Funktionstasten (F1-F12)
  - `Ctrl+Alt+Del` Sequenz
  - Beliebige Key-Kombinationen
- **Maus-Interaktion** - Volle Mouse-Unterstützung (Bewegung, Click, Scroll)
- **Echtzeit-Rendering** - Browser-basierter Remote-Desktop

### Container-State Management

Beim Öffnen von `/image/{id}`:
```typescript
// Register selected image
this.containerControlService.setSelectedImage(image);
```

Bei Component-Zerstörung:
```typescript
ngOnDestroy() {
  // Cleanup
  this.vncService.disconnect();
  this.containerControlService.clearSelectedImage();
}
```

Ermöglicht komponentenübergreifenden State-Tracking.

---

## Admin-Panel

### Zentrale Verwaltungsoberfläche

**Komponente:** `src/app/components/admin/admin.ts`  
**Service:** `src/app/service/admin.service.ts`

Das Admin-Panel ist ein vollständiges CRUD-Interface mit **7 Tabs**. Alle Daten werden beim Laden parallel per `Promise.all` vom Backend abgerufen.

**Allgemeine Eigenschaften:**
- Paralleles Laden aller Ressourcen bei Komponenten-Init
- Real-time Erfolgs- und Fehlermeldungen
- Loading-State verhindert Doppel-Submission
- Auto-befüllte Dropdowns aus geladenen Daten
- Manuelle ID-Eingabe als Fallback
- Bestätigungsdialoge vor Lösch-Operationen

### Tab 1: Kursverwaltung

**API:** `/api/courses`

| Aktion | Details |
|--------|---------|
| **Kurs erstellen** | Name (Pflichtfeld), Beschreibung (optional) |
| **Alle Kurse anzeigen** | Tabelle mit ID, Name, Beschreibung |
| **Kurs löschen** | Mit Bestätigungsdialog → `DELETE /api/courses/{id}` |

**API-Aufrufe:**
```
GET /api/courses - Alle Kurse laden
POST /api/courses - Neuen Kurs erstellen {name, description}
DELETE /api/courses/{id} - Kurs löschen
```

### Tab 2: Docker-Images

**API:** `/api/images`

| Aktion | Details |
|--------|---------|
| **Image erstellen** | Name (z.B. "Ubuntu"), Image-Referenz (z.B. `ubuntu:22.04`) |
| **Alle Images anzeigen** | Tabelle mit verfügbaren Container-Images |
| **Image löschen** | Mit Bestätigungsdialog |

**Beispiel-Images:**
- `ubuntu:22.04` - Ubuntu Linux
- `nginx:latest` - Nginx Webserver
- `node:20` - Node.js Runtime

**API-Aufrufe:**
```
GET /api/images - Alle verfügbaren Images
POST /api/images - Neues Image registrieren {name, url}
DELETE /api/images/{id} - Image entfernen
```

### Tab 3: Live-Umgebungen (VNC)

**API:** `/api/live-environments`

| Aktion | Details |
|--------|---------|
| **Umgebung erstellen** | VNC-Konfiguration eingeben |
| **Alle Umgebungen anzeigen** | Status-Badges zeigen Verbindungsstatus |
| **Umgebung löschen** | Mit Bestätigungsdialog |

**Erforderliche Felder beim Erstellen:**
- `userId` - Benutzer dem die Umgebung gehört
- `vncPort` - Port für VNC-Verbindung (z.B. 5900)
- `vncHost` - VNC-Server Hostname (z.B. localhost)
- `vncPassword` - VNC-Zugriffspasswort
- `status` - Verbindungsstatus (active/inactive/stopped)

**API-Aufrufe:**
```
GET /api/live-environments - Alle Umgebungen auflisten
POST /api/live-environments - Neue Umgebung erstellen
DELETE /api/live-environments/{id} - Umgebung löschen
```

### Tab 4: Benutzerverwaltung

**API:** `/api/users`

| Aktion | Details |
|--------|---------|
| **Benutzer erstellen** | Vollständige Registrierung |
| **Alle Benutzer anzeigen** | Mit Rollenbadges |
| **Benutzer löschen** | Mit Bestätigungsdialog |

**Erforderliche Felder:**
- `name` - Vollständiger Name
- `email` - E-Mail-Adresse (Validierung)
- `password` - Passwort (Hashing auf Backend)
- `className` - Schulklasse (optional)
- `role` - SCHUELER, LEHRER oder ADMIN
- `expiredAt` - Ablaufdatum (optional)

**API-Aufrufe:**
```
GET /api/users - Alle Benutzer auflisten
POST /api/users - Neuen Benutzer erstellen
DELETE /api/users/{id} - Benutzer löschen
```

### Tab 5: Aufgaben/Quizze

**API:** `/api/tasks`

| Aktion | Details |
|--------|---------|
| **Aufgabe erstellen** | Quiz-Metadaten definieren |
| **Alle Aufgaben anzeigen** | Tabellarische Übersicht |
| **Aufgabe löschen** | Mit Bestätigungsdialog |

**Erforderliche Felder:**
- `title` - Aufgaben-Titel
- `description` - Aufgabenbeschreibung (optional)
- `points` - Maximale Punkte
- `imageId` - Zugehöriges Docker-Image

**Features:**
- Dropdown zur Image-Auswahl (oder manuelle ID-Eingabe)
- Aufgaben können mehrere Fragen haben

**API-Aufrufe:**
```
GET /api/tasks - Alle Aufgaben auflisten
POST /api/tasks - Neue Aufgabe erstellen
DELETE /api/tasks/{id} - Aufgabe löschen
```

### Tab 6: Einschreibung & Kurszuordnung

**API:** `/api/student-courses`, `/api/course-tasks`

**A) Einzelne Benutzer einschreiben:**
- Benutzer-Dropdown (aus Benutzerliste)
- Kurs-Dropdown (aus Kursliste)
- Optional: Ablaufdatum setzen
- Button "Einschreiben" → `POST /api/student-courses`

**B) Ganze Klasse einschreiben:**
- Klassen-Dropdown (automatisch aus Benutzerliste extrahiert)
- Zeigt Benutzerzahl der ausgewählten Klasse
- Optional: Ablaufdatum setzen
- Button "Klasse einschreiben" → `POST /api/student-courses/enroll-class`
- Rückgabe: Erfolgreich eingeschriebene + übersprungene Benutzer

**C) Aufgabe einem Kurs zuordnen:**
- Kurs-Dropdown
- Aufgabe-Dropdown
- Reihenfolge-Index (orderIndex)
- `POST /api/course-tasks` {courseId, taskId, orderIndex}

**API-Aufrufe:**
```
POST /api/student-courses - Einzelnen Benutzer einschreiben
POST /api/student-courses/enroll-class - Ganze Klasse einschreiben
DELETE /api/student-courses/{id} - Einschreibung aufheben
POST /api/course-tasks - Aufgabe zu Kurs zuordnen
GET /api/course-tasks/course/{courseId}/tasks - Tasks eines Kurses
```

### Tab 7: Fragen/Quiz-Inhalte

**API:** `/api/questions`

| Aktion | Details |
|--------|---------|
| **Frage erstellen** | Komplexe Quiz-Definition |
| **Alle Fragen anzeigen** | Mit aufgelösten Aufgabennamen |
| **Frage löschen** | Mit Bestätigungsdialog |

**Erforderliche Felder:**
- `taskId` - Zugehörige Aufgabe (Dropdown)
- `frage` - Frage-Text
- `antworten` - JSON-Array mit Antworten
- `bestehgrenzeProzent` - Bestehengrenze (z.B. 70%)
- `maximalpunkte` - Maximale Punkte für diese Frage

**Antwort-JSON Format:**
```json
[
  {
    "text": "Die richtige Antwort",
    "richtig": true,
    "punkte": 10
  },
  {
    "text": "Eine falsche Antwort",
    "richtig": false,
    "punkte": 0
  }
]
```

**API-Aufrufe:**
```
GET /api/questions - Alle Fragen auflisten
GET /api/questions/task/{taskId} - Fragen einer Aufgabe
POST /api/questions - Neue Frage erstellen
DELETE /api/questions/{id} - Frage löschen
```

---

## Routing & Navigation

### Route-Übersicht

**Datei:** `src/app/app.routes.ts`

| Route | Komponente | Guard | Zweck |
|-------|-----------|-------|-------|
| `/login` | LoginComponent | — | Benutzer-Authentifizierung |
| `/` | — | — | Redirect zu `/dashboard` |
| `/dashboard` | DashboardComponent | authGuard ✓ | Kurse & Aufgaben ansehen |
| `/task/:taskId` | TaskDetail | authGuard ✓ | Quiz/Fragen bearbeiten |
| `/image/:id` | ImageComponent | authGuard ✓ | VNC-Viewer öffnen |
| `/admin` | Admin | authGuard ✓ | Admin-Panel (ohne Rollen-Check) |

**AuthGuard Logik:**
```typescript
if (authService.isLoggedIn()() && !authService.isTokenExpired()) {
  return true;  // Zugriff erlaubt
}
authService.logout();
return false;  // Redirect zu /login
```

⚠️ **Sicherheitshinweis:** `/admin` sollte zusätzlich eine Rollen-Guard haben.

### Programmatische Navigation

**Beispiele aus dem Code:**
```typescript
// Nach erfolgreicher Login
this.router.navigate(['/dashboard']);

// Zu Aufgabe
this.router.navigate(['/task', taskId]);

// Nach Quiz-Completion
this.router.navigate(['/image', imageId]);

// Zurück zum Dashboard
this.router.navigate(['/dashboard']);
```

---

## Service-Architektur

### AuthService

**Pfad:** `src/app/service/auth.service.ts`

**Verantwortlichkeiten:**
- JWT-Token-Verwaltung
- Benutzer-Authentifizierung
- Token-Validierung und -Parsing
- Auto-Logout bei Ablauf
- Rollen-Extraktion aus JWT-Claims

**Öffentliche API:**
```typescript
login(email: string, password: string): Promise<boolean>
logout(): void
isLoggedIn(): WritableSignal<boolean>
getToken(): ReadonlySignal<string>
getUserId(): ReadonlySignal<string>
getRoles(): ReadonlySignal<string[]>
isAdmin(): ComputedSignal<boolean>
isTeacher(): ComputedSignal<boolean>
isStudent(): ComputedSignal<boolean>
isTokenExpired(token?: string): boolean
```

**Signals:**
- `token` - JWT-Token String
- `userId` - Benutzer-Identität
- `roles` - Rollen-Array

**Computed Signals:**
- `isLoggedIn()` - true wenn Token existiert
- `isAdmin()` - true wenn ADMIN-Rolle vorhanden
- `isTeacher()` - true wenn LEHRER-Rolle vorhanden
- `isStudent()` - true wenn SCHUELER-Rolle vorhanden

### PermissionService

**Pfad:** `src/app/service/permission.service.ts`

**Verantwortlichkeiten:**
- Granulare Berechtigungsprüfungen
- UI-Visibilität basierend auf Rollen

**Berechtigungen (Computed Signals):**
```typescript
canRead()          // true wenn Benutzer angemeldet
canWrite()         // true wenn ADMIN
canGrade()         // true wenn LEHRER oder ADMIN
canManageContainer(ownerId) // true wenn ADMIN oder Besitzer
```

Automatisch reaktiv - kein manuelles Subscribe.

### ApiService

**Pfad:** `src/app/service/api.service.ts`

**Verantwortlichkeiten:**
- Zentralisierter Axios HTTP-Client
- JWT-Interceptor für Authorization-Header
- Konsistente Base-URL Konfiguration

**Konfiguration:**
- Base URL: `http://localhost:5050`
- Content-Type: `application/json`

**JWT-Interceptor:**
```typescript
// Jeden Request um Authorization-Header erweitern
Authorization: Bearer {JWT-Token aus sessionStorage}
```

**Nutzer:** AdminService, DashboardComponent, TaskDetail

⚠️ **Hinweis:** AuthService und ContainerControlService verwenden direktes Axios (andere Basis-URLs).

### AdminService

**Pfad:** `src/app/service/admin.service.ts`

**Verantwortlichkeiten:**
- CRUD-Operationen für alle Ressourcentypen
- Paralleles Laden aller Daten
- Verwaltungs-API-Aufrufe

**Datenladung:**
```typescript
loadAll(): Promise<AdminData>
  // Parallel via Promise.all():
  // - GET /api/courses
  // - GET /api/images
  // - GET /api/tasks
  // - GET /api/users
  // - GET /api/live-environments
  // - GET /api/questions
```

**Ressourcentypen mit CRUD:**

| Ressource | Create | Read | Update | Delete |
|-----------|--------|------|--------|--------|
| Kurse | `createCourse()` | ✓ | — | `deleteCourse()` |
| Images | `createImage()` | ✓ | — | `deleteImage()` |
| Live Envs | `createLiveEnv()` | ✓ | — | `deleteLiveEnv()` |
| Benutzer | `createUser()` | ✓ | — | `deleteUser()` |
| Aufgaben | `createTask()` | ✓ | — | `deleteTask()` |
| Fragen | `createQuestion()` | ✓ | — | `deleteQuestion()` |
| Einschreibung | `enrollSingleUser()`, `enrollByClass()` | — | — | — |
| Course-Task | `assignTaskToCourse()` | — | — | — |

### ExerciseService

**Pfad:** `src/app/service/exercise.service.ts`

**Verantwortlichkeiten:**
- Übungs-Fortschritts-Verwaltung
- Status-Updates
- Fortschritts-Berechnung

⚠️ **Status:** Aktuell hardcodierte Demo-Daten (3 Übungen), keine Backend-Integration

**Demo-Übungen:**
1. ITSI 9.1 Netzwerkforensik
2. ITSI 9.2 Memory Forensics
3. ITSI 9.3 Malware-Analyse Android

**Methoden:**
```typescript
getExercises(): ReadonlySignal<Exercise[]>
updateProgress(id: string, progress: number): void
  // 0 = not-started, 50 = in-progress, 100 = completed
updateStatus(id: string, status: string): void
  // Status → automatische Progress-Berechnung
markBewertet(id: string): void
```

### ContainerControlService

**Pfad:** `src/app/service/container-control.service.ts`

**Verantwortlichkeiten:**
- Docker-Container Lebenszyklus-Steuerung
- Container-Status-Verwaltung
- Ausgewähltes Image State-Management

**Container-Steuerung:**
```typescript
startContainer(userId: number, imageId: number): Promise<any>
  // POST /api/container/start {userId, imageId}
  
stopContainer(userId: number, imageId: number): Promise<any>
  // POST /api/container/stop {userId, imageId}
  
restartContainer(userId: number, imageId: number): Promise<any>
  // POST /api/container/reset {userId, imageId}
```

**State-Management:**
```typescript
setSelectedImage(image: Image): void
clearSelectedImage(): void
state$: Observable<ContainerControlState>
  // {selectedImage: Image | null, isActive: boolean}
```

**API-Basis:** `http://localhost:9090/api/container`

### VncService

**Pfad:** `src/app/service/vnc.service.ts`

**Verantwortlichkeiten:**
- noVNC (RFB) Protokoll-Management
- WebSocket-Verbindungshandling
- Echtzeit-Status-Updates

**RFB-Loading:**
- Sucht nach `window.RFB` (noVNC 1.3.0)
- Polling alle 100ms, Timeout nach 10 Sekunden
- Dynamic loading support

**Verbindungs-Methoden:**
```typescript
async connect(target: HTMLElement, options: VncOptions): Promise<void>
  // Verbindung aufbauen und RFB-Events registrieren
  
disconnect(): void
  // Sauberes Trennen
  
sendCtrlAltDel(): void
  // Ctrl+Alt+Del senden
  
sendKey(keysym: number, down: boolean): void
  // Individuelle Key-Events
```

**Status Observable:**
```typescript
status$: BehaviorSubject<VNCConnectionStatus>
  // DISCONNECTED | CONNECTING | CONNECTED | ERROR
```

**Event-Handler:**
- `connect` - Status → CONNECTED
- `disconnect` - Status → DISCONNECTED, cleanup
- `credentialsrequired` - Auto-send password
- `securityfailure` - Status → ERROR

---

## Zustandsverwaltung

### Angular Signals

**Einsatz:** Lokale Komponentenzustände und globale Auth-States

**Vorteile:**
- Reaktiv und performant
- Keine manuellen Subscribe nötig
- Computed Signals für abgeleitete Werte
- Fine-grained reactivity

**Beispiele:**
```typescript
// Auth-Signals (AuthService)
token = signal<string>('');
userId = signal<string>('');
roles = signal<string[]>([]);

// Komponenten-Signals (DashboardComponent)
dashboardCourses = signal<DashboardCourse[]>([]);
isLoading = signal(true);
errorMessage = signal('');

// Computed Signals (Permission-basiert)
canGrade = computed(() => 
  this.auth.isTeacher()() || this.auth.isAdmin()()
);
```

### RxJS Observables

**Einsatz:** Asynchrone Streams und Service-Status

**Vorteile:**
- Subscription-Management
- Operatoren für Transformation
- Hot/Cold streams
- Multi-subscriber support

**Beispiele:**
```typescript
// VNC Status Stream
this.vncService.status$.subscribe(status => {
  this.connectionStatus.set(status);
});

// Container State Stream
this.containerService.state$.pipe(
  map(state => state.selectedImage)
).subscribe(image => {
  this.selectedImage = image;
});
```

### Daten-Fluss-Muster

```
┌─────────────────────────────────────────────────────┐
│ User-Aktion (Click, Input)                         │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ Signal/Observable Update                           │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ Computed Signal reagiert (abhängige Werte)         │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ Template aktualisiert automatisch                  │
└─────────────────────────────────────────────────────┘
```

### Hybrid State Management

Die Anwendung kombiniert:
- **Signals** für UI-State und Auth-State
- **Observables** für Service-Status und async Events
- **Local Component State** für Formular-Daten
- **SessionStorage** für Token-Persistenz

---

## HTTP-Kommunikation & APIs

### ApiService - Zentralisierter HTTP-Client

**Datei:** `src/app/service/api.service.ts`

**Basis-Konfiguration:**
- Base URL: `http://localhost:5050`
- Content-Type: `application/json`
- HTTP-Bibliothek: Axios

**JWT-Interceptor:**
```typescript
// Jeder Request bekommt automatisch:
Authorization: Bearer {JWT-Token}
```

Token wird aus `sessionStorage` gelesen (Key: `auth_token`).

### API-Endpunkte Übersicht

**Base URLs:**
- Main API: `http://localhost:5050`
- Auth & Container: `http://localhost:9090`

**Auth-Endpunkte:**
```
POST /api/auth/login
  Body: {email, password}
  Response: {token: "JWT-String"}
```

**Kurs-Endpunkte:**
```
GET /api/courses - Alle Kurse
GET /api/courses/{id} - Kurs-Details
POST /api/courses - Neuer Kurs {name, description}
DELETE /api/courses/{id} - Kurs löschen
```

**Aufgaben & Fragen:**
```
GET /api/tasks - Alle Aufgaben
GET /api/tasks/{taskId} - Aufgabe laden
GET /api/questions/task/{taskId} - Fragen einer Aufgabe
POST /api/tasks - Aufgabe erstellen
POST /api/questions - Frage erstellen
DELETE /api/tasks/{id} - Aufgabe löschen
DELETE /api/questions/{id} - Frage löschen
```

**Einschreibung & Kurszuordnung:**
```
GET /api/student-courses/user/{userId} - Benutzer-Kurse
POST /api/student-courses - Einzelne Einschreibung
POST /api/student-courses/enroll-class - Klassen-Einschreibung
DELETE /api/student-courses/{id} - Einschreibung aufheben
POST /api/course-tasks - Task zu Kurs zuordnen
GET /api/course-tasks/course/{courseId}/tasks - Tasks eines Kurses
```

**Container-Steuerung:**
```
POST /api/container/start {userId, imageId}
POST /api/container/stop {userId, imageId}
POST /api/container/reset {userId, imageId}
```

**Docker-Images:**
```
GET /api/images - Alle verfügbaren Images
POST /api/images - Image erstellen
DELETE /api/images/{id} - Image löschen
```

**Live-Umgebungen (VNC):**
```
GET /api/live-environments - Alle Umgebungen
GET /api/live-environment/vnc-port/{userId} - VNC-Verbindungsinfo
POST /api/live-environments - Umgebung erstellen
DELETE /api/live-environments/{id} - Umgebung löschen
```

### Fehlerbehandlung

- API-Fehler werden gefangen und in `errorMessage` Signalen angezeigt
- Loading-States verhindern UI-Interaktion während Anfragen
- Benutzer sehen aussagekräftige Fehlermeldungen
- HTTP-Status Codes werden berücksichtigt

---

## Komponenten-Übersicht

### LoginComponent

**Pfad:** `src/app/components/login/login.component.ts`

**Funktion:** Authentifizierungs-Interface

**Features:**
- Email & Passwort Eingabefelder (Two-Way-Binding)
- Form-Validierung
- Fehlermeldung-Anzeige
- Loading-State während Submission
- Auto-Navigation zu Dashboard bei Erfolg
- Doppel-Click-Schutz

**Signals:**
```typescript
email: WritableSignal<string>
password: WritableSignal<string>
errorMessage: WritableSignal<string>
isSubmitting: WritableSignal<boolean>
```

**Methoden:**
```typescript
onSubmit(): void
  // 1. isSubmitting setzen (Doppel-Click-Schutz)
  // 2. AuthService.login() aufrufen
  // 3. Bei Erfolg: navigate to /dashboard
  // 4. Bei Fehler: errorMessage setzen
```

### DashboardComponent

**Pfad:** `src/app/components/dashboard/dashboard.component.ts`

**Funktion:** Hauptseite nach Login - Kurse und Aufgaben

**Features:**
- Kurs-Übersicht mit parallelem Laden
- Rollenbasierte UI-Elemente
- Aufgaben-Statistiken
- Navigation zu Tasks
- Admin-Panel Link (Admins only)
- Logout-Button

**Signals:**
```typescript
dashboardCourses: WritableSignal<DashboardCourse[]>
isLoading: WritableSignal<boolean>
errorMessage: WritableSignal<string>
roles: ReadonlySignal<string[]>  // from AuthService

// Computed Signals
isAdmin: ComputedSignal<boolean>
roleLabel: ComputedSignal<string>
stats: ComputedSignal<ExerciseStats>
```

**Methoden:**
```typescript
loadCourses(): void
  // Paralleles Laden aller Kurs-Daten
  
navigateToTask(taskId: number): void
navigateToImage(imageId: number): void
logout(): void
```

### TaskDetail Component

**Pfad:** `src/app/components/task-detail/task-detail.ts`

**Funktion:** Interaktives Quiz-System

**Features:**
- Schritt-für-Schritt Fragen-Navigation
- Sofortfeedback (richtig/falsch)
- Retry-Mechanismus für falsche Antworten
- Fortschrittsbalken
- Quiz-Abschluss mit Success-Meldung
- Flexible Antwort-Formate (JSON/Array)

**Signals:**
```typescript
task: WritableSignal<Task>
questions: WritableSignal<Question[]>
currentIndex: WritableSignal<number>
selectedAnswerIndex: WritableSignal<number | null>
feedback: WritableSignal<'correct' | 'wrong' | null>
isLoading: WritableSignal<boolean>

// Computed Signals
currentQuestion: ComputedSignal<Question>
parsedAnswers: ComputedSignal<Answer[]>
allAnswered: ComputedSignal<boolean>
progressPercent: ComputedSignal<number>
```

**Methoden:**
```typescript
selectAnswer(index: number): void
submitAnswer(): void
  // Validates selected answer
  // Sets feedback signal
goBack(): void
navigateToImage(): void
```

### ImageComponent (VNC-Viewer)

**Pfad:** `src/app/components/image/imageComponent.ts`

**Funktion:** Remote-Desktop Viewer

**Features:**
- VNC-Verbindungsaufbau
- Echtzeit-Status-Anzeige
- Loading-Spinner während Verbindungsversuch
- Canvas-Rendering für Remote-Desktop
- Auto-Cleanup bei Component-Zerstörung

**Signals:**
```typescript
imageId: WritableSignal<number>
image: WritableSignal<Image>
isConnecting: WritableSignal<boolean>
connectionStatus: WritableSignal<string>
```

**ViewChild:**
```typescript
@ViewChild('vncScreen') vncScreen: ElementRef;
```

**Methoden:**
```typescript
setupVnc(): void
  // 1. VNC-Port abrufen
  // 2. VncService.connect() aufrufen
  // 3. Status abonnieren
```

### AdminComponent

**Pfad:** `src/app/components/admin/admin.ts`

**Funktion:** 7-Tab Admin-Dashboard

**Features:**
- Tab-Navigation zwischen 7 Resourcen
- Paralleles Laden aller Daten
- Real-time Erfolgs-/Fehlermeldungen
- Loading-State und Error-Handling
- Dropdowns mit Auto-Populierung
- Bestätigungsdialoge vor Delete

**Signals:**
```typescript
adminData: WritableSignal<AdminData>
activeTab: WritableSignal<number>
loadingTab: WritableSignal<boolean>
errorMessage: WritableSignal<string>
successMessage: WritableSignal<string>
```

**Methoden:**
```typescript
loadAllData(): void
  // Promise.all([
  //   GET /api/courses,
  //   GET /api/images,
  //   GET /api/tasks,
  //   GET /api/users,
  //   GET /api/live-environments,
  //   GET /api/questions
  // ])
```

---

## Datenmodelle

### Exercise Model

```typescript
interface Exercise {
  id: string;
  title: string;
  description?: string;
  progress: number;      // 0-100
  status: 'not-started' | 'in-progress' | 'completed';
  category?: string;
  imageId?: string;
  bewertet?: boolean;    // Teacher graded?
}
```

### Image Model

```typescript
interface Image {
  ID: bigint;
  Name: string;
  URL: string;           // Docker image reference (e.g., "ubuntu:22.04")
  Instances?: Instance[];
}
```

### User Model

```typescript
interface User {
  id: number;
  name: string;
  email: string;
  password?: string;      // Nur bei Erstellung
  className?: string;
  role?: string;          // SCHUELER, LEHRER, ADMIN
  createdAt?: string | null;
  expiredAt?: string | null;
}
```

### Course Models

```typescript
interface Course {
  id: number;
  name: string;
  description?: string;
}

interface CourseTask {
  id: number;
  title: string;
  description?: string;
  points: number;
  imageId?: number;
  orderIndex?: number;
}

interface DashboardCourse {
  courseId: number;
  courseName: string;
  courseDescription?: string;
  enrolledAt: string;
  expiresAt?: string;
  tasks: CourseTask[];
}

interface StudentCourse {
  userId: number;
  courseId: number;
  enrolledAt: string;
  expiresAt?: string;
}
```

### Task & Question Models

```typescript
interface Task {
  id: number;
  title: string;
  description?: string;
  points: number;
  imageId: number;
}

interface Answer {
  text: string;
  richtig: boolean;     // Correct?
  punkte: number;       // Points for this answer
}

interface Question {
  id: number;
  taskId: number;
  frage: string;        // Question text
  antworten: Answer[] | string;  // JSON or array
  bestehgrenzeProzent: number;   // Passing percentage
  maximalpunkte: number;         // Max points
}
```

### Container Model

```typescript
interface Instance {
  instanceID: bigint;
  instanceName: string;
  status: string;        // e.g., "running", "stopped"
  user_id: bigint;
  image_id: bigint;
}
```

---

## TypeScript & Konfiguration

### Strict Mode

**tsconfig.json:**
```json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitOverride": true,
    "noPropertyAccessFromIndexSignature": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "target": "ES2022",
    "module": "ES2022"
  }
}
```

**Effekte:**
- Vollständige Typprüfung erzwungen
- Keine impliziten `any`-Typen
- Alle Codepfade müssen Werte zurückgeben
- Switch-Cases dürfen nicht durchfallen
- Override-Keyword erforderlich

### Angular-Konfiguration

**angular.json:**
- Build-Output: `dist/itsi-container-frontend/browser/`
- Development Server Port: 4200
- Production Optimization
- Assets: noVNC files included

---

## Docker & Deployment

### Multi-Stage Docker Build

**Dockerfile:**

**Stage 1: Builder** (Node 22 Alpine)
```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build
OUTPUT: dist/itsi-container-frontend/browser/
```

**Stage 2: Runtime** (Nginx 1.27 Alpine)
```dockerfile
FROM nginx:1.27-alpine
COPY --from=builder /app/dist/itsi-container-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 9090
CMD ["nginx", "-g", "daemon off;"]
```

### Nginx-Konfiguration

**Datei:** `nginx.conf`

**Konfiguration:**
- Port: 9090
- Proxy zu Backend API
- WebSocket-Unterstützung
- SPA-Fallback (alle Routes → index.html)

**Proxy-Regeln:**
```nginx
location /api/ {
  proxy_pass http://steuerung:9090;
}

location /ws/ {
  proxy_pass http://steuerung:9090;
  proxy_http_version 1.1;
  proxy_set_header Upgrade $http_upgrade;
  proxy_set_header Connection "upgrade";
}

location / {
  try_files $uri $uri/ /index.html;
}
```

### Build & Deployment Commands

```bash
# Development
npm install
ng serve                              # Port 4200

# Production Build
npm run build
  # → dist/itsi-container-frontend/browser/

# Docker Image bauen
docker build -t itsi-container-frontend .

# Docker Container starten
docker run -p 9090:9090 itsi-container-frontend
  # → Verfügbar auf http://localhost:9090
```

---

## Bekannte Einschränkungen

| Bereich | Status | Details | Priorität |
|---------|--------|---------|-----------|
| **ExerciseService** | ⚠️ Hardcodiert | 3 Demo-Übungen; keine API-Integration | Hoch |
| **ImageComponent Images** | ⚠️ Mock-Daten | 3 hardcodierte Images; sollte `/api/images/{id}` aufrufen | Mittel |
| **Admin-Route Guard** | ⚠️ Sicherheit | Nur AuthGuard, keine Rollen-Prüfung → Schüler könnten `/admin` aufrufen | Hoch |
| **Hardcodierte URLs** | ⚠️ Config | `localhost:9090`, `localhost:5050` im Code | Mittel |
| **getAufgabe.ts** | ⚠️ Unused | Dummy-URL Klasse, kann entfernt werden | Niedrig |
| **API Konsistenz** | ℹ️ Note | AuthService nutzt direktes Axios; andere ApiService | Niedrig |
| **NoVncService** | ℹ️ Minimal | Lightweight Factory, kaum verwendet | Niedrig |

### Recommended Fixes

1. **Admin Role Guard hinzufügen:**
   ```typescript
   if (!authService.isAdmin()()) {
     return false;  // Deny access for non-admins
   }
   ```

2. **ExerciseService mit Backend verbinden:**
   ```typescript
   loadExercises(): void {
     this.http.get('/api/exercises/user/:userId')
       .subscribe(data => this.exercises.set(data));
   }
   ```

3. **ImageComponent Dynamic Loading:**
   ```typescript
   loadImage(): void {
     const id = this.route.snapshot.paramMap.get('id');
     this.http.get(`/api/images/${id}`)
       .subscribe(image => this.image.set(image));
   }
   ```

4. **Environment-basierte URLs:**
   - Konfiguration in `environment.ts` / `environment.prod.ts`
   - Beim Build: Auto-Replacement
   - z.B. `https://api.example.com` für Production

---

**Dokumentation:** März 2026  
**Angular Version:** 21.0.0  
**TypeScript Version:** 5.9.2  
**Status:** Produktionsreif (mit bekannten Verbesserungen)

**Service:** `src/app/service/auth.service.ts`

| Feature | Details |
|---|---|
| **JWT-Login** | `POST /api/auth/login` via Axios; Antwort enthält `token` |
| **Token-Speicherung** | `sessionStorage` (Key: `auth_token`) – wird bei Tab-Schließen automatisch gelöscht |
| **Token-Parsing** | Base64url-Dekodierung des JWT-Payloads, unterstützt die Claims `sub`, `email`, `userId`, `roles`, `role`, `rolle` |
| **Rollen-Unterstützung** | `SCHUELER`, `LEHRER`, `ADMIN` |
| **Auto-Logout** | Polling alle **30 Sekunden** prüft den `exp`-Claim; bei Ablauf → automatischer Logout + Weiterleitung zu `/login` |
| **Reaktive Signale** | `token`, `userId`, `roles` als Angular Signals; berechnete Signale `isLoggedIn()`, `isAdmin()`, `isTeacher()`, `isStudent()` |
| **Logout** | Löscht alle Signale + `sessionStorage`, navigiert zu `/login` |

---

## 2. Berechtigungssystem

**Service:** `src/app/service/permission.service.ts`

| Permission | Bedingung |
|---|---|
| `canRead` | Benutzer ist eingeloggt |
| `canWrite` | Benutzer hat Rolle `ADMIN` |
| `canGrade` | Benutzer hat Rolle `LEHRER` **oder** `ADMIN` |
| `canManageContainer(ownerId)` | `ADMIN` immer, sonst nur wenn eigene `userId === ownerId` |

Alle Permissions sind als **Angular Computed Signals** implementiert (reaktiv, kein manuelles Subscribe nötig).

---

## 3. Routing & Route-Guards

**Dateien:** `src/app/app.routes.ts`, `src/app/guards/auth.guard.ts`

| Route | Komponente | Guard |
|---|---|---|
| `/login` | `LoginComponent` | – |
| `/` | → Redirect zu `/dashboard` | – |
| `/dashboard` | `DashboardComponent` | `authGuard` ✓ |
| `/task/:taskId` | `TaskDetail` | `authGuard` ✓ |
| `/image/:id` | `ImageComponent` | `authGuard` ✓ |
| `/admin` | `Admin` | `authGuard` ✓ |

**AuthGuard:** Prüft `isLoggedIn()` **und** `isTokenExpired()`. Bei ungültigem Token wird `logout()` aufgerufen und zu `/login` weitergeleitet.

---

## 4. Login-Seite

**Komponente:** `src/app/components/login/`

- E-Mail & Passwort Eingabefelder (Two-Way-Binding via `FormsModule`)
- Fehlermeldung bei falschen Credentials: *„Falsche E-Mail oder Passwort"*
- Loading-State (`isSubmitting`): verhindert Doppel-Klick
- Bei Erfolg → automatische Weiterleitung zu `/dashboard`

---

## 5. Dashboard

**Komponente:** `src/app/components/dashboard/`  
**Service:** `ExerciseService`, `AuthService`, `PermissionService`

### 5.1 Kurs- und Aufgaben-Übersicht
- Lädt beim Start alle **eingeschriebenen Kurse** des eingeloggten Benutzers (`GET /api/student-courses/user/:userId`)
- Für jeden Kurs wird der Kursname (`GET /api/courses/:id`) und die zugehörigen Aufgaben (`GET /api/course-tasks/course/:courseId/tasks`) nachgeladen
- Zeigt Kursname, Einschreibedatum, Ablaufdatum und alle Tasks an
- Klick auf einen Task → Navigation zu `/task/:taskId`

### 5.2 Rollen-basierte UI
| Element | Sichtbar für |
|---|---|
| Admin-Button | Nur `ADMIN` |
| Bewerten-Button | `LEHRER` und `ADMIN` (`canGrade`) |
| Status ändern | Nur `ADMIN` (`canWrite`) |
| Rollen-Label | Alle (zeigt aktuell eingeloggte Rolle) |

### 5.3 Aufgaben-Statistik (lokaler `ExerciseService`)
- Zählt Aufgaben nach Status: **Nicht gestartet**, **In Arbeit**, **Abgeschlossen**
- Klick auf Exercise → Navigation zu `/image/:imageId`

### 5.4 Navigation
- **Admin-Panel** Button (nur für Admins)
- **Logout** Button

---

## 6. Admin-Panel

**Komponente:** `src/app/components/admin/`  
**Service:** `src/app/service/admin.service.ts`

Das Admin-Panel ist ein vollständiges CRUD-Interface mit **7 Tabs**. Alle Daten werden beim Laden parallel per `Promise.all` vom Backend abgerufen. Erfolgs- und Fehlermeldungen werden inline angezeigt.

### Tab 1 – Kurse (`/api/courses`)
| Aktion | Details |
|---|---|
| **Erstellen** | Name (Pflichtfeld), Beschreibung (optional) |
| **Übersicht** | Listet alle Kurse mit ID, Name, Beschreibung |
| **Löschen** | Bestätigungsdialog → `DELETE /api/courses/:id` |

### Tab 2 – Docker Images (`/api/images`)
| Aktion | Details |
|---|---|
| **Erstellen** | Name, Image-Referenz (z. B. `ubuntu:22.04`) |
| **Übersicht** | Tabelle mit ID, Name, Image-Referenz |
| **Löschen** | Bestätigungsdialog → `DELETE /api/images/:id` |

### Tab 3 – Live-Umgebungen (`/api/live-environments`)
| Aktion | Details |
|---|---|
| **Erstellen** | User-ID, VNC-Port, VNC-Host, VNC-Passwort, Status (`active` / `inactive` / `stopped`) |
| **Übersicht** | Tabelle mit ID, User-ID, VNC-Host, VNC-Port, Status-Badge |
| **Löschen** | Bestätigungsdialog → `DELETE /api/live-environments/:id` |

### Tab 4 – Benutzer (`/api/users`)
| Aktion | Details |
|---|---|
| **Erstellen** | Name, E-Mail, Passwort, Klasse, Rolle (`SCHUELER` / `LEHRER` / `ADMIN`), optionales Ablaufdatum (`datetime-local`) |
| **Übersicht** | Tabelle mit ID, Name, E-Mail, Klasse, Rollen-Badge |
| **Löschen** | Bestätigungsdialog → `DELETE /api/users/:id` |

### Tab 5 – Aufgaben (`/api/tasks`)
| Aktion | Details |
|---|---|
| **Erstellen** | Titel, Beschreibung, Punkte, Docker-Image (Dropdown aus vorhandenen Images oder manuelle ID-Eingabe) |
| **Einem Kurs zuordnen** | Kurs-Dropdown + Task-Dropdown + Reihenfolge-Index → `POST /api/course-tasks` |
| **Übersicht** | Tabelle mit ID, Titel, Beschreibung, Punkte, Image-ID |
| **Löschen** | Bestätigungsdialog → `DELETE /api/tasks/:id` |

### Tab 6 – Kurszuordnung / Enrollment (`/api/student-courses`)
| Aktion | Details |
|---|---|
| **Einzelnen Benutzer einschreiben** | Benutzer-Dropdown, Kurs-Dropdown, optionales Ablaufdatum |
| **Gesamte Klasse einschreiben** | Klassen-Dropdown (automatisch aus Benutzerliste extrahiert), Kurs-Dropdown, optionales Ablaufdatum → `POST /api/student-courses/enroll-class` |
| **Ergebnis-Anzeige** | Zeigt an: Anzahl neu eingeschrieben ✓ und bereits vorhandene ⟳ |
| **Klassen-Vorschau** | Zählt Benutzer der gewählten Klasse live vor dem Einschreiben |

### Tab 7 – Fragen (`/api/questions`)
| Aktion | Details |
|---|---|
| **Erstellen** | Task-Dropdown, Fragetext, Antworten als JSON-Array (`[{"text":"…","correct":true}]`), Maximalpunkte, Bestehgrenze in % |
| **Übersicht** | Tabelle mit ID, zugehöriger Aufgabe (Name aufgelöst), Fragetext, Maximalpunkte, Bestehgrenze |
| **Löschen** | Bestätigungsdialog → `DELETE /api/questions/:id` |

---

## 7. Aufgaben-Detail & Quiz-System

**Komponente:** `src/app/components/task-detail/`

### Daten-Loading
- Lädt Aufgaben-Metadaten: `GET /api/tasks/:taskId`
- Lädt alle zugehörigen Fragen: `GET /api/questions/task/:taskId`
- Aufgabe ohne Fragen wird trotzdem korrekt angezeigt

### Quiz-Durchlauf
| Feature | Details |
|---|---|
| **Schrittweise Navigation** | Eine Frage pro Schritt, `currentIndex` Signal |
| **Antwort-Auswahl** | Klick auf Antwortoption; nur eine Antwort gleichzeitig auswählbar |
| **Sofort-Feedback** | Nach „Antwort abgeben": grünes ✓ (richtig) oder rotes ✗ (falsch) |
| **Retry** | Bei falscher Antwort → „Erneut versuchen" setzt Auswahl zurück |
| **Weiter** | Bei richtiger Antwort → nächste Frage |
| **Fortschrittsbalken** | Berechnet prozentualen Fortschritt (`currentIndex / total * 100`) |
| **Abschluss-Anzeige** | Nach letzter Frage: Erfolgsmeldung mit Button zur VNC-Umgebung |
| **Antwort-Parsing** | Antworten können als JSON-String oder direkt als Array gespeichert sein; beide Formate werden unterstützt |

### Navigation
- Zurück zum Dashboard: `goBack()`
- Nach Abschluss → Navigation zu `/image/:imageId` (zur VNC-Umgebung der Aufgabe)

---

## 8. VNC / Live-Umgebung (noVNC)

**Services:** `src/app/service/vnc.service.ts`, `src/app/service/novnc.ts`  
**Komponente:** `src/app/components/image/`

### VncService
| Feature | Details |
|---|---|
| **Verbindungsaufbau** | Lädt `window.RFB` (noVNC 1.3.0) dynamisch; Polling alle 100 ms, Timeout nach 10 s |
| **Verbindungs-Status** | RxJS `BehaviorSubject<VNCConnectionStatus>` (`DISCONNECTED`, `CONNECTING`, `CONNECTED`, `ERROR`) |
| **Viewport-Skalierung** | `scaleViewport` und `resizeSession` konfigurierbar |
| **Passwort-Authentifizierung** | `credentialsrequired`-Event → automatisches Senden des VNC-Passworts |
| **Sicherheitsfehler** | `securityfailure`-Event → Status `ERROR` |
| **Disconnect** | Sauberes Trennen via `rfb.disconnect()` |
| **Tastatur-Shortcuts** | `sendCtrlAltDel()`, `sendKey(keysym, down)` |

### NoVncService
- Einfache Factory-Methode `createRFBConnection(url, target)` als leichtgewichtige Alternative zum VncService

### ImageComponent (VNC-Ansicht)
- Ruft VNC-Port und Passwort ab: `GET /api/live-environment/vnc-port/:userId`
- Baut WebSocket-URL auf: `ws://localhost:9090/ws/novnc?vncPort=<port>`
- Zeigt VNC-Canvas in einem `<div #vncScreen>` an
- Verbindungsstatus wird live angezeigt
- Bei Zerstörung der Komponente: automatisches Disconnect + Zurücksetzen des Container-States

---

## 9. Container-Steuerung

**Service:** `src/app/service/container-control.service.ts`  
**Backend-Endpunkt:** `http://localhost:9090/api/container`

| Aktion | HTTP | Endpoint |
|---|---|---|
| **Container starten** | `POST` | `/api/container/start` mit `{ userId, imageId }` |
| **Container stoppen** | `POST` | `/api/container/stop` mit `{ userId, imageId }` |
| **Container zurücksetzen** | `POST` | `/api/container/reset` mit `{ userId, imageId }` |

- JWT-Token wird automatisch als `Authorization: Bearer …` Header angehängt
- **State-Management** via RxJS `BehaviorSubject<ContainerControlState>`:
  - `selectedImage`: aktuell gewähltes Docker-Image
  - `isActive`: ob eine Umgebung aktiv ist
- `setSelectedImage(image)` / `clearSelectedImage()` für komponentenübergreifenden State

---

## 10. API-Client (Axios)

**Datei:** `src/app/service/api.service.ts`

- Zentrale Axios-Instanz `apiClient` mit `baseURL: http://localhost:5050`
- **Request-Interceptor:** Liest `auth_token` aus `sessionStorage` und hängt `Authorization: Bearer <token>` automatisch an **jeden** Request an
- Wird von `AdminService`, `DashboardComponent` und `TaskDetail` genutzt

> **Hinweis:** `AuthService` und `ContainerControlService` verwenden direkt `axios` ohne den zentralen Client (abweichende Base-URLs: Port `9090` statt `5050`).

---

## 11. Datenmodelle

| Modell | Datei | Felder |
|---|---|---|
| `Exercise` | `models/exercise.model.ts` | `id`, `title`, `description`, `progress`, `status`, `category`, `imageId`, `bewertet` |
| `Image` | `models/Image.ts` | `ID` (BigInt), `Name`, `URL` |
| `DashboardCourse` | `models/DashboardCourse.ts` | `courseId`, `courseName`, `enrolledAt`, `expiresAt`, `tasks[]` |
| `CourseTask` | `models/DashboardCourse.ts` | `id`, `title`, `description`, `points`, `imageId`, `orderIndex` |
| `StudentCourse` | `models/StudentCourse.ts` | `courseId`, `enrolledAt`, `expiresAt` |
| `User` | `models/User.ts` | Benutzerdaten |
| `Instance` | `models/Instance.ts` | Container-Instanz-Daten |
| `Course` | `models/Course.ts` | Kursdaten |

---

## 12. Services – Übersicht

| Service | Zweck |
|---|---|
| `AuthService` | JWT-Login, Token-Verwaltung, Rollen, Auto-Logout |
| `PermissionService` | Berechtigungs-Computed-Signals (`canRead`, `canWrite`, `canGrade`, `canManageContainer`) |
| `AdminService` | CRUD-Operationen für Admin-Panel (Kurse, Images, User, Tasks, Fragen, Enrollments, Live-Envs) |
| `ApiService` | Zentrale Axios-Instanz mit JWT-Interceptor |
| `ExerciseService` | Lokaler Signal-Store für Übungsaufgaben (aktuell hardcodierte Demo-Daten) |
| `ContainerControlService` | Start / Stop / Reset von Docker-Containern, RxJS State |
| `VncService` | noVNC-Verbindungsmanagement (RFB), Status-Observable, Tastatur-Events |
| `NoVncService` | Leichtgewichtige RFB-Factory-Alternative |

---

## 13. Bekannte Einschränkungen / In Arbeit

| Bereich | Status | Details |
|---|---|---|
| `ExerciseService` | ⚠️ Hardcodiert | Demo-Daten (3 Aufgaben) sind fest im Code; noch keine API-Anbindung |
| `getAufgabe.ts` | ⚠️ Platzhalter | Klasse `getAufgaben` verwendet `https://api.example.com/aufgaben` als Dummy-URL; noch nicht in Verwendung |
| `ImageComponent` | ⚠️ Mock-Daten | Image-Daten (`Ubuntu 22.04`, `Nginx`, `Node.js`) sind hardcodiert; kein `GET /api/images/:id` Call |
| Admin-Route | ⚠️ Kein Rollen-Guard | `/admin` ist nur durch `authGuard` geschützt; ein eingeloggter `SCHUELER` könnte die URL direkt aufrufen |
| VNC-URL | ℹ️ Konfiguration | `ws://localhost:9090` ist hardcodiert; für Produktion muss eine Umgebungsvariable verwendet werden |
| Container-URL | ℹ️ Konfiguration | `http://localhost:9090/api/container` ist hardcodiert in `ContainerControlService` |
| Auth-Login-URL | ℹ️ Konfiguration | `http://localhost:9090/api/auth/login` ist hardcodiert in `AuthService` (nutzt nicht den zentralen `apiClient`) |

