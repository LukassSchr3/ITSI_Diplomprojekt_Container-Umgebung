FROM kalilinux/kali-rolling

ENV DEBIAN_FRONTEND=noninteractive

RUN apt update && apt upgrade -y && \
    apt install -y \
        kali-linux-headless \
        net-tools iproute2 iputils-ping \
        curl wget git sudo nano vim && \
    apt clean

CMD ["/bin/bash"]
