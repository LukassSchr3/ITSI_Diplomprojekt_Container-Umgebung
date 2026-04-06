FROM kalilinux/kali-rolling

ENV DEBIAN_FRONTEND=noninteractive
ENV DISPLAY=:99
ENV VNC_RESOLUTION=1280x720

RUN apt update && apt upgrade -y && \
    apt install -y \
        kali-linux-headless \
        xfce4 xfce4-terminal xfce4-goodies \
        x11vnc xvfb novnc websockify \
        net-tools iproute2 iputils-ping \
        curl wget git sudo nano vim \
        dbus-x11 supervisor \
    && apt clean && rm -rf /var/lib/apt/lists/*

# Copy startup script
COPY start-vnc.sh /opt/start-vnc.sh
RUN sed -i 's/\r$//' /opt/start-vnc.sh && chmod +x /opt/start-vnc.sh

# Optional: Configure x11vnc password (can be overridden by env var)
RUN mkdir -p ~/.vnc && \
    x11vnc -storepasswd kali ~/.vnc/passwd || true

EXPOSE 6080 5900

CMD ["/opt/start-vnc.sh"]