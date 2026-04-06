# ITSI Diplomprojekt – Container-Umgebung

Eine webbasierte Plattform zur Verwaltung von Docker-Container-Umgebungen für den Schulunterricht. Schülerinnen und Schüler können über einen Browser eigene Container starten, Übungen absolvieren und eine vollständige Kali-Linux-Desktop-Umgebung direkt im Browser nutzen.

---

## Inhaltsverzeichnis

- [Projektübersicht](#projektübersicht)
- [Architektur](#architektur)
- [Komponenten](#komponenten)
  - [Frontend (Angular)](#frontend-angular)
  - [Steuerung API (Spring Boot)](#steuerung-api-spring-boot)
  - [Container Backend (Go)](#container-backend-go)
  - [Datenbank API (Spring Boot)](#datenbank-api-spring-boot)
  - [PostgreSQL-Datenbank](#postgresql-datenbank)
  - [Live Environment (Kali Linux)](#live-environment-kali-linux)
- [Voraussetzungen](#voraussetzungen)
- [Projekt starten](#projekt-starten)
- [Ports im Überblick](#ports-im-überblick)
- [Benutzerrollen](#benutzerrollen)
- [Features](#features)

---

## Projektübersicht

Das Projekt ermöglicht Lehrkräften, Übungen und Quizze zu erstellen, denen Docker-Images zugeordnet werden. Schülerinnen und Schüler können die zugehörigen Container starten, zurücksetzen und direkt im Browser über VNC (noVNC) mit einer grafischen Kali-Linux-Umgebung arbeiten – ohne lokale Installation.

---

## Architektur

```
┌──────────────────────────────────────────────────────────────┐
│                    Browser (Port 9090)                        │
│                  Angular Frontend (SPA)                       │
└─────────────────────────┬────────────────────────────────────┘
                          │ HTTP / WebSocket
┌─────────────────────────▼────────────────────────────────────┐
│              Steuerung API – Spring Boot (Port 9090)          │
│   Authentifizierung (JWT) · Container-Orchestrierung          │
│   Live-Environment-Verwaltung · noVNC WebSocket-Proxy         │
└────────────┬──────────────────────────┬──────────────────────┘
             │ HTTP                     │ HTTP
┌────────────▼──────────┐   ┌──────────▼──────────────────────┐
│  Datenbank API        │   │  Container Backend – Go          │
│  Spring Boot          │   │  (Port 3030)                     │
│  (Port 5050)          │   │  Docker SDK · Image-/Container-  │
│  CRUD für Benutzer,   │   │  Verwaltung · Live Environments  │
│  Images, Instanzen,   │   └──────────┬──────────────────────┘
│  Live-Environments    │              │ Docker API
└────────────┬──────────┘   ┌──────────▼──────────────────────┐
             │              │  Docker Engine (Host)            │
┌────────────▼──────────┐   │  Container, Images              │
│  PostgreSQL (Port 5432)│  │  Live-Environment-Container      │
│  + Adminer (Port 8080) │  └─────────────────────────────────┘
└───────────────────────┘
```

---

## Komponenten

### Frontend (Angular)

**Verzeichnis:** `frontend/`  
**Technologie:** Angular 21, TypeScript, noVNC  
**Port (Produktion):** `9090` (via nginx)  
**Port (Entwicklung):** `4200`

Das Frontend ist eine Single-Page-Application (SPA) mit folgenden Bereichen:

| Route | Beschreibung |
|---|---|
| `/login` | Anmeldung mit E-Mail und Passwort |
| `/dashboard` | Übersicht der eigenen Übungen und Container |
| `/exercises` | Liste aller verfügbaren Übungen |
| `/exercises/:id` | Detailansicht einer Übung mit Container-Steuerung |
| `/image/:id` | Docker-Image-Detailansicht |
| `/quiz-start/:id` | Quiz starten |
| `/quiz/:id` | Quiz durchführen |
| `/task/:id` | Aufgaben-Detailansicht |
| `/progress` | Eigener Lernfortschritt (Schüler) |
| `/teacher-progress` | Lernfortschritt aller Schüler (Lehrkraft) |
| `/admin` | Admin-Bereich |
| `/admin/quiz` | Quiz-Verwaltung |
| `/admin/exercises` | Übungs-Verwaltung |

**Entwicklungsserver starten:**
```bash
cd frontend
npm install
npm start          # http://localhost:4200
```

**Produktions-Build:**
```bash
npm run build
```

**Docker-Image bauen:**
```bash
docker build -t itsi-frontend ./frontend
```

---

### Steuerung API (Spring Boot)

**Verzeichnis:** `api/steuerung/`  
**Technologie:** Spring Boot, Java, JWT  
**Port:** `9090`

Die Steuerung API ist das Herzstück der Anwendung. Sie verbindet das Frontend mit dem Container-Backend und der Datenbank-API.

**Hauptfunktionen:**
- JWT-basierte Authentifizierung (`POST /api/auth/login`)
- Container starten, stoppen und zurücksetzen (`/api/container/*`)
- Live-Environments verwalten (`/api/live-environment/*`)
- WebSocket-Proxy für noVNC (`ws://localhost:9090/ws/novnc?vncPort=<port>`)
- Status-Updates via WebSocket (`ws://localhost:9090/ws/live-environment/{userId}`)

**Starten (IntelliJ oder Gradle):**
```bash
cd api/steuerung
./gradlew bootRun
```

**Swagger UI:** `http://localhost:9090/swagger-ui/index.html`

**Abhängigkeiten:**
- Datenbank API muss auf Port `5050` erreichbar sein
- Container Backend muss auf Port `3030` erreichbar sein

---

### Container Backend (Go)

**Verzeichnis:** `backend/`  
**Technologie:** Go 1.25, Gin, Docker SDK  
**Port:** `3030`

Das Backend kommuniziert direkt mit der Docker Engine des Hosts und stellt eine REST-API bereit.

**API-Endpunkte:**

| Methode | Endpunkt | Beschreibung |
|---|---|---|
| `GET` | `/health` | Health-Check |
| `GET` | `/images/` | Alle Docker-Images auflisten |
| `POST` | `/images/add` | Image von Docker Hub herunterladen |
| `POST` | `/images/update` | Image aktualisieren (Pull + Replace) |
| `DELETE` | `/images/remove` | Image entfernen |
| `POST` | `/images/inspect` | Image-Details abrufen |
| `GET` | `/instances/` | Alle Container auflisten |
| `POST` | `/instances/start` | Container erstellen und starten |
| `POST` | `/instances/stop` | Container stoppen |
| `POST` | `/instances/reset` | Container zurücksetzen (neu erstellen) |
| `GET` | `/live/` | Alle Live-Environments auflisten |
| `GET` | `/live/status/:name` | Status einer Live-Environment |
| `POST` | `/live/start` | Live-Environment für einen User starten |
| `POST` | `/live/stop` | Live-Environment stoppen |
| `POST` | `/live/reset` | Live-Environment zurücksetzen |

**Starten (lokal):**
```bash
cd backend
go run .
```

**Docker-Image bauen:**
```bash
docker build -t itsi-backend ./backend
```

**Voraussetzung:** Docker Engine muss auf dem Host laufen und über den Unix-Socket erreichbar sein (`/var/run/docker.sock`).

> Das Backend bereinigt inaktive Live-Environment-Container automatisch alle **30 Minuten**. Container, die länger als **2 Stunden** gestoppt sind, werden entfernt.

---

### Datenbank API (Spring Boot)

**Verzeichnis:** `api/database/`  
**Technologie:** Spring Boot, Java, PostgreSQL  
**Port:** `5050`

Stellt CRUD-Endpunkte für alle Datenbank-Entitäten bereit.

**Entitäten:** Benutzer, Images, Instanzen, Live-Environments

**Starten:**
```bash
cd api/database
./gradlew bootRun
```

**Datenbank starten (PostgreSQL via Docker):**
```bash
cd api/database
docker-compose up -d
```

**Zugangsdaten PostgreSQL:**

| Parameter | Wert |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Datenbank | `itsi_db` |
| Benutzer | `itsi_user` |
| Passwort | `itsi_password` |

**Adminer (Datenbankverwaltung):** `http://localhost:8080`

---

### PostgreSQL-Datenbank

**Verzeichnis:** `api/database/`  
**Port:** `5432`

Die Datenbank wird via Docker Compose gestartet und enthält alle persistenten Daten der Anwendung. Das Datenbankschema (`schema.sql`) wird beim ersten Start automatisch eingespielt.

```bash
# Starten
docker-compose up -d

# Stoppen
docker-compose down

# Vollständig zurücksetzen (alle Daten löschen)
docker-compose down -v && docker-compose up -d
```

---

### Live Environment (Kali Linux)

**Verzeichnis:** `backend/live-environment/`

Jede Schülerin und jeder Schüler erhält auf Anfrage einen eigenen Kali-Linux-Container mit grafischem Desktop (XFCE4), der direkt im Browser über noVNC genutzt werden kann.

**Enthaltene Software:**
- Kali Linux (`kali-linux-headless`)
- XFCE4 Desktop-Umgebung
- x11vnc (VNC-Server, Port `5900`)
- noVNC + websockify (Browser-Zugriff, Port `6080`)
- Tools: `curl`, `wget`, `git`, `nano`, `vim`, `net-tools`

**VNC-Passwort:** `kali` (fest im Image gespeichert)

**Image bauen (zwingend vor dem ersten Start!):**
```bash
cd backend/live-environment
docker build -t kali-liveenv:latest .
```

Alternativ mit `make`:
```bash
make build    # Image bauen
make run      # Container starten
make logs     # Logs anzeigen
make shell    # Shell öffnen
make clean    # Container und Image entfernen
```

> Das Image muss **vor dem Start des Backends** gebaut werden, da das Backend das Image beim Starten einer Live-Environment voraussetzt.

---

## Voraussetzungen

- **Docker** (Engine + CLI) – für Container und Live-Environments
- **Java 21+** – für Steuerung API und Datenbank API
- **Go 1.25+** – für das Container Backend (oder Docker nutzen)
- **Node.js 22+ / npm 10+** – für das Frontend (oder Docker nutzen)
- **Docker Compose** – für die PostgreSQL-Datenbank

---

## Projekt starten

### Schritt 1 – Datenbank starten

```bash
cd api/database
docker-compose up -d
```

### Schritt 2 – Kali Live-Environment Image bauen

```bash
cd backend/live-environment
docker build -t kali-liveenv:latest .
```

### Schritt 3 – Datenbank API starten

```bash
cd api/database
./gradlew bootRun
# Läuft auf http://localhost:5050
```

### Schritt 4 – Container Backend starten

```bash
cd backend
go run .
# Läuft auf http://localhost:3030
```

> Alternativ als Docker-Container:
> ```bash
> docker build -t itsi-backend ./backend
> docker run -p 3030:3030 -v /var/run/docker.sock:/var/run/docker.sock itsi-backend
> ```

### Schritt 5 – Steuerung API starten

```bash
cd api/steuerung
./gradlew bootRun
# Läuft auf http://localhost:9090
```

### Schritt 6 – Frontend starten

**Entwicklungsmodus:**
```bash
cd frontend
npm install
npm start
# Läuft auf http://localhost:4200
```

**Oder als Docker-Container (Produktion):**
```bash
docker build -t itsi-frontend ./frontend
docker run -p 9090:9090 itsi-frontend
# Läuft auf http://localhost:9090
```

---

## Ports im Überblick

| Dienst | Port | Beschreibung |
|---|---|---|
| Frontend (Entwicklung) | `4200` | Angular Dev Server |
| Frontend / Steuerung API | `9090` | Nginx + Spring Boot |
| Container Backend | `3030` | Go REST API |
| Datenbank API | `5050` | Spring Boot REST API |
| PostgreSQL | `5432` | Datenbank |
| Adminer | `8080` | Datenbankverwaltung (Web UI) |
| noVNC (Live-Env) | dynamisch | Per User zugewiesen (ab Port 6080) |
| VNC direkt | dynamisch | Per User zugewiesen (ab Port 5900) |

---

## Benutzerrollen

| Rolle | Berechtigungen |
|---|---|
| **Schüler** | Dashboard, Übungen, Quizze, eigene Container, Live-Environment, Lernfortschritt |
| **Lehrer / Admin** | Alles wie Schüler + Übungsverwaltung, Quiz-Verwaltung, Schülerfortschritt, Image-Verwaltung |

---

## Features

- 🔐 **Authentifizierung** – Login mit E-Mail/Passwort, JWT-basiert
- 📚 **Übungen & Quizze** – Erstellen, zuweisen und absolvieren
- 🐳 **Docker-Image-Verwaltung** – Images herunterladen, aktualisieren, entfernen
- 📦 **Container-Instanzen** – Starten, stoppen und zurücksetzen per Klick
- 🖥️ **Live Kali-Linux-Desktop** – Vollständige grafische Umgebung im Browser via noVNC
- 📁 **Datei-Upload** – Dateien direkt in Container hochladen
- 📊 **Lernfortschritt** – Schülerfortschritt für Lehrkräfte einsehbar
- 🧹 **Automatische Bereinigung** – Inaktive Container werden nach 2 Stunden entfernt
