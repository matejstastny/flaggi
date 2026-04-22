#!/usr/bin/env bash

source "$(dirname "$0")/lib/logging.sh"

CONTAINER_ID=$(
    docker ps --format "{{.ID}} {{.Image}}" |
        grep "^.* vsc-flaggi" |
        awk '{print $1}' |
        head -n 1
)

if [ -z "$CONTAINER_ID" ]; then
    log_err "No running dev container found for flaggi"
    log_err "Make sure VS Code has reopened the project in the container"
    exit 1
fi

docker exec -it -u flaggi "$CONTAINER_ID" bash -l
