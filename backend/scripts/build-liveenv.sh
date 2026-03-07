#!/bin/bash

echo "Building Kali Live Environment Docker Image..."
docker build -t kali-liveenv:latest ./live-environment

if [ $? -eq 0 ]; then
    echo "Successfully built kali-liveenv:latest"
    docker images | grep kali-liveenv
else
    echo "Failed to build image"
    exit 1
fi