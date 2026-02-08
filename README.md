# ITSI Database API

Dieses Projekt stellt eine REST-API für die Verwaltung von Benutzern, Images, Instanzen und Live-Umgebungen bereit. Die Anwendung basiert auf Spring Boot und verwendet eine H2-Datenbank.

## Features
- CRUD-Operationen für Benutzer, Images, Instanzen und Live-Umgebungen
- Schnittstelle für das Frontend zur Verwaltung der Datenbank
- Endpunkt zur Abfrage des maximalen VNC-Ports in Live-Umgebungen
- H2-Konsole zur Datenbankverwaltung
- OpenAPI/Swagger UI zur API-Dokumentation

## Endpunkte (Beispiele)

### Benutzer
- `GET /api/users` – Alle Benutzer abrufen
- `POST /api/users` – Benutzer anlegen
- `PUT /api/users/{id}` – Benutzer aktualisieren
- `DELETE /api/users/{id}` – Benutzer löschen

### Images
- `GET /api/images` – Alle Images abrufen
- `POST /api/images` – Image anlegen
- `PUT /api/images/{id}` – Image aktualisieren
- `DELETE /api/images/{id}` – Image löschen

### Instanzen
- `GET /api/instances` – Alle Instanzen abrufen
- `POST /api/instances` – Instanz anlegen
- `PUT /api/instances/{id}` – Instanz aktualisieren
- `DELETE /api/instances/{id}` – Instanz löschen

### Live-Umgebungen
- `GET /api/live-environments` – Alle Live-Umgebungen abrufen
- `POST /api/live-environments` – Live-Umgebung anlegen
- `PUT /api/live-environments/{id}` – Live-Umgebung aktualisieren
- `DELETE /api/live-environments/{id}` – Live-Umgebung löschen
- `GET /api/live-environments/max-vnc-port` – Maximalen VNC-Port abrufen

## H2-Konsole
Die H2-Konsole ist unter `/h2-console` erreichbar. Die Zugangsdaten findest du in der `application.properties`.

## Swagger UI
Die API-Dokumentation ist unter `/swagger-ui.html` verfügbar.

## Datenbankschema
Das Schema befindet sich in `src/main/resources/schema.sql` und wird beim Start automatisch angewendet.

## Starten der Anwendung
1. Stelle sicher, dass Java und Gradle installiert sind.
2. Starte die Anwendung z.B. mit `./gradlew bootRun` oder über deine IDE.
3. Die API ist standardmäßig auf Port 8080 erreichbar (konfigurierbar in `application.properties`).

## Hinweise
- Die Anwendung verwendet eine H2-Datenbank, die beim Start automatisch initialisiert wird.
- Für produktive Nutzung sollte eine persistente Datenbank verwendet werden.


# Steuerung API

## Übersicht

Diese API ist Teil eines Diplomprojekts und dient als Middleware zwischen Frontend, Datenbank-API und Backend (z.B. Go-Backend für Containersteuerung). Sie bietet REST- und WebSocket-Schnittstellen für Container- und Live-Environment-Management sowie File-Uploads.

## Features

- Container starten, stoppen, zurücksetzen
- File-Upload für Container (inkl. automatischer Instanz-Erstellung)
- Live-Environment-Management (Start, Stop, Create)
- Dynamische WebSocket-Kanäle für Live-Environment-Status und noVNC
- Automatische Vergabe von VNC- und noVNC-Ports
- Swagger UI für API-Dokumentation

## Endpunkte (Auszug)

### Container
- `POST /api/container/start` – Startet einen Container
- `POST /api/container/stop` – Stoppt einen Container
- `POST /api/container/reset` – Setzt einen Container zurück
- `POST /api/container/upload` – Datei-Upload für einen Container (benötigt userId, imageId)

### Live-Environment
- `POST /api/live-environment/start/{userId}` – Startet oder erstellt ein Live-Environment für einen User
- `POST /api/live-environment/stop/{userId}` – Stoppt das Live-Environment
- `POST /api/live-environment/create` – Erstellt ein neues Live-Environment (benötigt vncPassword)

### WebSocket
- `/ws/live-environment/{userId}` – Dynamischer WebSocket für Live-Environment-Status
- `/ws/novnc?vncPort=59XX` – WebSocket für noVNC (Port dynamisch, siehe Live-Environment)

## File-Upload Beispiel (curl)

```
curl -X POST http://localhost:9090/api/container/upload \
  -F "file=@/pfad/zur/deiner/datei.txt" \
  -F "userId=1" \
  -F "imageId=42"
```

## Live-Environment Beispiel

1. Live-Environment erstellen:
   ```
   curl -X POST http://localhost:9090/api/live-environment/create \
     -H "Content-Type: application/json" \
     -d '{"userId":1, "vncPassword":"meinPasswort"}'
   ```
2. Live-Environment starten:
   ```
   curl -X POST http://localhost:9090/api/live-environment/start/1
   ```

## Swagger UI

Swagger UI ist unter `http://localhost:9090/swagger-ui/index.html` erreichbar (sofern aktiviert).

## Hinweise

- Die API erwartet, dass die Datenbank-API auf Port 5050 läuft und das Backend (z.B. Go) erreichbar ist.
- Die container_id wird beim Erstellen einer Instanz automatisch nach dem Muster `cont_XXX` vergeben (höchste ID + 1).
- Für noVNC wird der Port nach dem Muster 60XX vergeben, wobei XX die letzten beiden Ziffern des VNC-Ports sind.

## Entwicklung & Start

- Das Projekt ist ein Spring Boot-Projekt und kann direkt in IntelliJ IDEA gestartet werden (kein globales Gradle nötig).
- Für ein ausführbares JAR: Rechtsklick auf das Projekt > "Build Artifact" > "Build" oder über das Gradle-Toolwindow `bootJar` ausführen.



