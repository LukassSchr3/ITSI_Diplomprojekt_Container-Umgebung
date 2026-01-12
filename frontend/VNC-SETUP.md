# noVNC Setup für ITSI Container Frontend

## Übersicht

Die Anwendung verwendet noVNC, um über WebSockets eine Verbindung zu VNC-Servern herzustellen.

## Architektur

### VNC Service (JavaScript)

Der VNC Service ist in **JavaScript** (`vnc.service.js`) statt TypeScript implementiert, um:
- ✅ Bessere Kompatibilität mit noVNC (CommonJS-Modul)
- ✅ Vermeidung von top-level await Problemen
- ✅ Direkte Verwendung des nativen noVNC RFB-Imports
- ✅ TypeScript-Definitionen sind separat verfügbar (`.d.ts`)

## VNC-Konfiguration

### 1. Backend/VNC-Server Setup

Für jedes Container-Image muss ein VNC-Server konfiguriert werden. Dies geschieht normalerweise auf dem Backend.

**Beispiel VNC WebSocket URL:**
```
ws://localhost:6080
```

### 2. Frontend-Konfiguration

Die VNC-URL wird in `src/app/image/imageComponent.ts` konfiguriert:

```typescript
private connectVNC(): void {
  // Passe die URL an dein Backend an
  const vncUrl = `ws://localhost:6080`;
  
  this.rfb = new RFB(this.vncScreen.nativeElement, vncUrl, {
    credentials: { password: '' },
  });
}
```

### 3. Dynamische VNC-URLs

Für produktive Umgebungen solltest du die VNC-URL basierend auf dem Image dynamisch generieren:

```typescript
private connectVNC(): void {
  const imageId = this.imageId();
  // Beispiel: Verschiedene Ports für verschiedene Container
  const vncUrl = `ws://localhost:${6080 + Number(imageId)}`;
  
  // Oder: URL vom Backend abrufen
  // const vncUrl = this.image()?.vncUrl;
  
  this.rfb = new RFB(this.vncScreen.nativeElement, vncUrl, {
    credentials: { password: 'your-password' },
  });
}
```

### 4. Backend-Integration

Erweitere das `Image` Interface um VNC-Informationen:

```typescript
export interface Image {
  ID: bigint;
  Name: string;
  URL: string;
  VNCUrl?: string;  // WebSocket URL für VNC
  VNCPassword?: string;  // Optional: VNC Passwort
  Instances?: Instance[];
}
```

## VNC-Server Beispiel (Docker Container)

### Dockerfile mit VNC

```dockerfile
FROM ubuntu:22.04

# VNC Server installieren
RUN apt-get update && apt-get install -y \
    x11vnc \
    xvfb \
    websockify

# VNC Server starten
CMD Xvfb :99 -screen 0 1024x768x16 & \
    x11vnc -display :99 -forever -shared & \
    websockify --web=/usr/share/novnc 6080 localhost:5900
```

### Docker Compose Beispiel

```yaml
version: '3.8'
services:
  ubuntu-vnc:
    build: ./ubuntu-vnc
    ports:
      - "6080:6080"
      - "5900:5900"
    environment:
      - DISPLAY=:99
```

## Funktionen

### Aktuelle Features

- ✅ Klickbare Image-Liste im Dashboard
- ✅ Navigation zu VNC-Ansicht beim Klick
- ✅ noVNC Integration
- ✅ Verbindungsstatus-Anzeige
- ✅ Automatische Skalierung
- ✅ Zurück-Navigation zum Dashboard

### VNC-Einstellungen

Die folgenden Einstellungen sind aktiviert:

```typescript
this.rfb.scaleViewport = true;  // Automatische Skalierung
this.rfb.resizeSession = true;  // Session-Größe anpassen
```

## Troubleshooting

### Problem: "Verbindung fehlgeschlagen"

**Lösung:**
1. Stelle sicher, dass der VNC-Server läuft
2. Überprüfe die WebSocket-URL
3. Prüfe die Browser-Konsole für Fehler
4. Stelle sicher, dass websockify läuft

### Problem: "Authentifizierung fehlgeschlagen"

**Lösung:**
1. Überprüfe das VNC-Passwort
2. Aktualisiere die credentials in `connectVNC()`

### CORS-Probleme

Wenn der VNC-Server auf einem anderen Host läuft, stelle sicher, dass CORS korrekt konfiguriert ist.

## Nächste Schritte

1. **Backend-API erstellen** für dynamische Image-Daten
2. **VNC-URLs** vom Backend abrufen
3. **Authentifizierung** implementieren
4. **Container-Management** hinzufügen (Start/Stop)
5. **Session-Verwaltung** für mehrere gleichzeitige Verbindungen

