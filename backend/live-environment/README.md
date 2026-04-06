# Live Environment – Dockerfile

Dieses Dockerfile baut das Docker-Image `kali-liveenv:latest` für die Live Environment.  
Jeder User bekommt einen eigenen Container mit einem vollständigen Kali-Linux-Desktop, der über VNC im Browser erreichbar ist.

---

## Image bauen

```bash
docker build -t kali-liveenv:latest .
```

> Das Image muss **vor dem Start des Backends** gebaut werden, da der Backend-Service das Image beim Start einer Live Environment voraussetzt.

---

## Was ist enthalten

- **Kali Linux** (`kali-linux-headless`) als Basis
- **XFCE4** Desktop-Umgebung
- **x11vnc** VNC-Server (Port 5900)
- **noVNC + websockify** für den Browser-Zugriff (Port 6080)
- Werkzeuge: `curl`, `wget`, `git`, `nano`, `vim`, `net-tools`, `iproute2`

---

## VNC-Passwort

Das VNC-Passwort ist fix auf **`kali`** gesetzt und wird beim Image-Build gespeichert:

```dockerfile
RUN x11vnc -storepasswd kali ~/.vnc/passwd
```

Dieses Passwort gilt für alle Live Environments. Es kann nicht zur Laufzeit per Environment-Variable überschrieben werden.

---

## CRLF-Fix (Windows/Linux Kompatibilität)

Das Startup-Script `start-vnc.sh` wird unter Windows mit CRLF-Zeilenenden (`\r\n`) gespeichert.  
Linux erwartet jedoch LF (`\n`). Ohne Korrektur schlägt der Container-Start mit folgendem Fehler fehl:

```
exec /opt/start-vnc.sh: no such file or directory
```

Der Fehler wirkt irreführend (die Datei existiert ja), wird aber durch den `\r` am Ende der Shebang-Zeile (`#!/bin/bash\r`) verursacht – der Kernel findet den Interpreter `/bin/bash\r` nicht.

**Fix im Dockerfile:**
```dockerfile
COPY start-vnc.sh /opt/start-vnc.sh
RUN sed -i 's/\r$//' /opt/start-vnc.sh && chmod +x /opt/start-vnc.sh
```

`sed` entfernt alle `\r`-Zeichen beim Build, unabhängig davon ob das Image unter Windows oder Linux gebaut wird.

---

## Ports

| Port | Verwendung                        |
|------|-----------------------------------|
| 5900 | Direkter VNC-Zugriff (RFB-Proto)  |
| 6080 | noVNC WebSocket (Browser-Zugriff) |

---

## Makefile-Befehle

| Befehl          | Beschreibung                              |
|-----------------|-------------------------------------------|
| `make build`    | Image bauen                               |
| `make run`      | Container starten                         |
| `make shell`    | Shell im laufenden Container öffnen       |
| `make logs`     | Container-Logs anzeigen                   |
| `make rebuild`  | Image neu bauen (clean + build)           |
| `make clean`    | Container und Image entfernen             |
| `make stop`     | Container stoppen                         |
