#!/bin/bash
set -e

export DISPLAY=:99

echo "========================================"
echo "Starting Kali Live Environment"
echo "User: ${USER:-unknown}"
echo "Resolution: ${VNC_RESOLUTION:-1280x720}"
echo "========================================"

# Start Xvfb
echo "[1/4] Starting Xvfb virtual display..."
Xvfb :99 -screen 0 ${VNC_RESOLUTION:-1280x720}x24 -ac +extension GLX +render -noreset &
XVFB_PID=$!
sleep 2

# Start XFCE4
echo "[2/4] Starting XFCE4 desktop environment..."
startxfce4 &
sleep 3

# Start x11vnc
echo "[3/4] Starting x11vnc server..."
if [ -f ~/.vnc/passwd ]; then
    echo "Using VNC password authentication"
    x11vnc -display :99 -forever -shared -rfbport 5900 -rfbauth ~/.vnc/passwd &
else
    echo "Warning: No VNC password set (running without authentication)"
    x11vnc -display :99 -forever -shared -rfbport 5900 -nopw &
fi
X11VNC_PID=$!
sleep 2

# Start noVNC (websockify)
echo "[4/4] Starting noVNC (websockify)..."
echo "========================================"
echo "Live environment ready!"
echo "noVNC WebSocket: ws://localhost:6080"
echo "Direct VNC: localhost:5900"
echo "========================================"

websockify --web=/usr/share/novnc 6080 localhost:5900

# Cleanup on exit
trap "kill $XVFB_PID $X11VNC_PID 2>/dev/null" EXIT
wait