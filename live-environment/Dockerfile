FROM kalilinux/kali-rolling

ENV DEBIAN_FRONTEND=noninteractive

RUN apt update && apt upgrade -y && \
    apt install -y \
        kali-linux-headless \
        net-tools iproute2 iputils-ping \
        novnc \
        x11vnc \
        xvfb \
        websockify \
        curl wget git sudo nano vim && \
    apt clean

CMD Xvfb :99 -screen 0 1024x768x16 & \
    x11vnc -display :99 -forever -shared & \
    websockify --web=/usr/share/novnc 6080 localhost:5900

CMD ["/bin/bash"]
