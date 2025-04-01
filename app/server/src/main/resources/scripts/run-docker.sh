#!/bin/bash

set -e
set -u

# ───────────────────────────────────────────────────────────────────────────────
# Docker run Script for "Flaggi" server
# Runs server Docker container, using the name and exposing the port defined
# in `config.properties`
# ───────────────────────────────────────────────────────────────────────────────

# ─── Config values ─────────────────────────────────────────────────────────────

DOCKER_NAME=$(awk -F'=' '/^docker.name=/ {print $2}' config.properties)
TCP_PORT=$(awk -F'=' '/^tcp.port=/ {print $2}' config.properties)
UDP_PORT=$(awk -F'=' '/^udp.port=/ {print $2}' config.properties)

# ─── Functions ─────────────────────────────────────────────────────────────────

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo " -h, --help            Display this help message"
    echo " -r, --rebuild         Rebuilds the Docker image."
    exit 0
}

log_info() { echo "$(date +'%Y-%m-%d %H:%M:%S') 📦  $1"; }
log_warn() { echo "$(date +'%Y-%m-%d %H:%M:%S') ⚠️  $1"; }
log_success() { echo "$(date +'%Y-%m-%d %H:%M:%S') ✅  $1"; }
log_error() {
    echo "$(date +'%Y-%m-%d %H:%M:%S') ❌  $1" >&2
    exit 1
}

# ─── Flags ─────────────────────────────────────────────────────────────────────

REBUILD="false"
while [[ "$#" -gt 0 ]]; do
    case "$1" in
    -h | --help)
        usage
        ;;
    -r | --rebuld)
        REBUILD="true"
        ;;
    *)
        log_error "Unknown argument: $1"
        ;;
    esac
    shift
done

# ─── Dependencies ──────────────────────────────────────────────────────────────

if [ ! -f "config.properties" ]; then
    log_error "config.properties not found! Ensure the file exists before starting the container."
fi

if [ -z "$DOCKER_NAME" ]; then
    echo "❌  Missing required configuration values in config.properties."
    echo "    Configure field 'docker.name'"
    exit 1
elif [ -z "$TCP_PORT" ]; then
    echo "❌  Missing required configuration values in config.properties."
    echo "    Configure field 'tcp.port'"
    exit 1
elif [ -z "$UDP_PORT" ]; then
    echo "❌  Missing required configuration values in config.properties."
    echo "    Configure field 'udp.port'"
    exit 1
fi

echo "✅  Configuration loaded: DOCKER_NAME=$DOCKER_NAME, TCP_PORT=$TCP_PORT, UDP_PORT=$UDP_PORT"

if ! command -v docker >/dev/null 2>&1; then
    echo "❌  Docker is not installed. Please install Docker first."
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "❌  Docker daemon is not running. Please start Docker and try again."
    exit 1
fi

# ─── Docker check ──────────────────────────────────────────────────────────────

if ! docker image inspect "$DOCKER_NAME" >/dev/null 2>&1; then
    echo "❌  Docker image '$DOCKER_NAME' not found."
    read -p "Do you want to build the Docker image? (y/n) " response
    if [ "$response" = "y" ]; then
        echo "🛠️  Building Docker image '$DOCKER_NAME'..."
        docker build -t "$DOCKER_NAME" .
    else
        exit 1
    fi
fi

if [ "$REBUILD" == "true" ]; then
    echo "🛠️  Building Docker image '$DOCKER_NAME'..."
    docker build -t "$DOCKER_NAME" .
fi

if docker ps -a --format '{{.Names}}' | grep -q "^$DOCKER_NAME$"; then
    echo "⚠️  Container '$DOCKER_NAME' already exists."
    echo "📦  Stopping and removing existing container..."
    docker stop "$DOCKER_NAME" >/dev/null 2>&1
    docker rm "$DOCKER_NAME" >/dev/null 2>&1
fi

# ─── Docker run ────────────────────────────────────────────────────────────────

echo "📦  Starting Docker container '$DOCKER_NAME'..."
printf "%$(tput cols)s" '' | tr ' ' -
docker run --name "$DOCKER_NAME" \
    -p "$TCP_PORT:$TCP_PORT" \
    -p "$UDP_PORT:$UDP_PORT/udp" \
    -v "$(pwd)/config.properties:/app/config.properties" \
    "$DOCKER_NAME"

echo "✅  Docker container '$DOCKER_NAME' started successfully."
