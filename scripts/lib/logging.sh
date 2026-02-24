#!/usr/bin/env bash

# --------------------------------------------------------------------------------------------
# logging.sh — Logging Functions
# --------------------------------------------------------------------------------------------
# Author: Matej Stastny
# Date: 2025-09-12 (YYYY-MM-DD)
# License: MIT
# Link: https://github.com/matejstastny/flaggi
# --------------------------------------------------------------------------------------------

log() {
    local level="$1"
    shift
    local label

    case "$level" in
    info) label="INFO" ;;
    done) label="DONE" ;;
    prompt) label="INPT" ;;
    warn) label="WARN" ;;
    error) label="ERROR" ;;
    *) label="LOG  " ;;
    esac

    echo "[$label] $*" >&2
}

die() {
    log "error" "$*"
    exit 1
}
