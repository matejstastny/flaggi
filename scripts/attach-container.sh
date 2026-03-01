#!/usr/bin/env bash

# --------------------------------------------------------------------------------------------
# attach-container.sh - attaches to vscode devcontainer
# --------------------------------------------------------------------------------------------
# Author: Matej Stastny
# Date: 2025-11-23 (YYYY-MM-DD)
# License: MIT
# Link: https://github.com/matejstastny/flaggi
# --------------------------------------------------------------------------------------------

source "$(dirname "$0")/lib/logging.sh"

CONTAINER_ID=$(
    docker ps --format "{{.ID}} {{.Image}}" |
        grep "^.* vsc-flaggi" |
        awk '{print $1}' |
        head -n 1
)

if [ -z "$CONTAINER_ID" ]; then
    log error "No running dev container found for flaggi"
    log error "Make sure VS Code has reopened the project in the container"
    exit 1
fi

docker exec -it -u flaggi "$CONTAINER_ID" bash -l
