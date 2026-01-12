
**Fehlerantworten:**
- `400 Bad Request`: Ungültige Anfrage
- `404 Not Found`: Container existiert nicht
- `409 Conflict`: Container läuft nicht
- `500 Internal Server Error`: Container konnte nicht gestoppt werden

---

### Instanz zurücksetzen

**Endpunkt:** `POST /instances/reset`

**Status:** Noch nicht implementiert

---

## Live-Umgebung

Die folgenden Endpunkte sind für die Verwaltung von Live-Entwicklungsumgebungen vorgesehen.

### Live-Umgebung starten

**Endpunkt:** `POST /live/start`

**Status:** Noch nicht implementiert

---

### Live-Umgebung stoppen

**Endpunkt:** `POST /live/stop`

**Status:** Noch nicht implementiert

---

### Live-Umgebung zurücksetzen

**Endpunkt:** `POST /live/reset`

**Status:** Noch nicht implementiert

---

## Hinweise

- Alle Endpunkte verwenden JSON als Request/Response-Format
- Stellen Sie sicher, dass der Docker-Daemon läuft und erreichbar ist
- Der Server läuft standardmäßig auf Port `3030`

---

## Beispiel-Workflows

### Workflow: Neuen Container erstellen und starten

1. **Image prüfen/laden:**
   ```bash
   POST /images/add
   {
     "name": "nginx-server",
     "imageRef": "nginx:latest"
   }
   ```

2. **Container erstellen und starten:**
   ```bash
   POST /instances/start
   {
     "name": "web-server-1",
     "imageRef": "nginx:latest",
     "imageId": 1,
     "userId": 1
   }
   ```

3. **Status überprüfen:**
   ```bash
   GET /instances/
   ```

### Workflow: Container stoppen und Image entfernen

1. **Container stoppen:**
   ```bash
   POST /instances/stop
   {
     "name": "web-server-1"
   }
   ```

2. **Image entfernen:**
   ```bash
   DELETE /images/remove
   {
     "imageRef": "nginx:latest"
   }
   ```

---

**Autor:** Karol Gradkowski