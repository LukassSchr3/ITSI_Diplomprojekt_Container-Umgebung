# Steuerung API – Dokumentation

Middleware-API zwischen Frontend, Datenbank-API (Port 5050) und Container-Backend (Port 3030).

**Base URL:** `http://localhost:9090`  
**Swagger UI:** `http://localhost:9090/swagger-ui.html`

---

## Authentifizierung

Alle Endpoints außer `/api/auth/**` und `/api/info` erfordern einen gültigen JWT-Token im Header:

```
Authorization: Bearer <token>
```

---

## Endpoints

### Auth – `/api/auth`

#### `POST /api/auth/login`
Authentifiziert einen User und gibt einen JWT-Token zurück.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "geheim"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Login successful",
  "token": "<jwt-token>",
  "user": {
    "id": 1,
    "name": "Max Muster",
    "email": "user@example.com",
    "className": "5AHIT",
    "role": "student",
    "createdAt": "...",
    "expiredAt": "..."
  }
}
```

**Fehler:** `400` bei fehlenden Feldern, `401` bei falschen Credentials.

---

#### `GET /api/auth/validate`
Prüft ob ein JWT-Token gültig ist.

**Header:** `Authorization: Bearer <token>`

**Response (200):** `{ "success": true, "message": "Token is valid" }`  
**Response (401):** Token ungültig oder abgelaufen.

---

### Container – `/api/container`

Steuert Docker-Container-Instanzen für User/Image-Kombinationen. Der Container-Name wird aus Username und Image-Name gebildet (`username_imagename`).

**Request-Body für alle Operationen:**
```json
{
  "userId": 1,
  "imageId": 5
}
```

**Response:**
```json
{
  "success": true,
  "message": "...",
  "containerId": "cont_42",
  "status": "running",
  "containerIp": "172.17.0.5",
  "instance": { ... }
}
```

#### `POST /api/container/start`
Startet den Container für einen User + Image. Legt die Instanz in der Datenbank an, falls noch nicht vorhanden.

#### `POST /api/container/stop`
Stoppt den laufenden Container und aktualisiert den Status in der Datenbank.

#### `POST /api/container/reset`
Stoppt den Container, entfernt ihn und startet ihn neu (fresh state).

---

### Live Environment – `/api/live-environment`

Verwaltet persönliche Kali-Linux-Desktops pro User. Jede Live Environment läuft als Docker-Container mit VNC-Zugang.

#### `POST /api/live-environment/start/{userId}`
Startet die Live Environment für den User. Legt automatisch einen neuen Eintrag in der Datenbank an (inkl. VNC-Port-Zuweisung), falls noch keiner existiert.

**Response (200):** Live-Environment-Objekt mit Status `running` und `noVncPort`.

#### `POST /api/live-environment/stop/{userId}`
Stoppt die laufende Live Environment.

#### `POST /api/live-environment/reset/{userId}`
Setzt die Live Environment zurück – bestehender Container wird entfernt und neu erstellt.

#### `POST /api/live-environment/create`
Legt manuell einen neuen Live-Environment-Eintrag an.

**Request:**
```json
{
  "userId": 1,
  "vncPassword": "geheim",
  "vncHost": "localhost",
  "vncPort": 5901
}
```
`vncHost` und `vncPort` sind optional – Defaults: `localhost`, `5900 + userId`.

#### `GET /api/live-environment/vnc-port/{userId}`
Gibt die VNC-Verbindungsdetails für einen User zurück.

**Response:**
```json
{
  "vncPort": 5901,
  "vncPassword": "geheim"
}
```

---

### API Info – `/api`

#### `GET /api/info`
Gibt eine Übersicht der API zurück (Version, Endpoints, Verbindungen zu Backend und Datenbank).

#### `GET /api/health`
Health-Check – gibt `{ "status": "UP" }` zurück.

---

## WebSocket

### `ws://localhost:9090/ws/live-environment/{userId}`
Statuskanal für die Live Environment eines Users. Beim Verbinden wird sofort der aktuelle Status aus der Datenbank gesendet. Bei Start/Stop/Reset-Operationen werden Updates automatisch gepusht.

**Nachrichtenformat (JSON):**
```json
{
  "id": 1,
  "userId": 1,
  "status": "running",
  "vncPort": 5901,
  "noVncPort": 6001
}
```

### `ws://localhost:9090/ws/novnc?vncPort=<port>`
Transparenter WebSocket-Proxy zum VNC-Server. Leitet den RFB-Protokoll-Stream direkt vom VNC-Container weiter. Wird vom noVNC-Client genutzt um den Desktop im Browser anzuzeigen.

**Query-Parameter:** `vncPort` – Port des VNC-Servers (z. B. `5901`). Falls nicht angegeben, wird der konfigurierte Default-Port verwendet.

---

## Ports & Abhängigkeiten

| Service           | Port |
|-------------------|------|
| Steuerung API     | 9090 |
| Datenbank API     | 5050 |
| Container Backend | 3030 |

VNC-Ports werden pro User ab dem Max-Port der Datenbank hochgezählt (Fallback: `5900 + userId`).
