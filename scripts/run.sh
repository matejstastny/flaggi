#!/usr/bin/env bash

set -e
source "$(dirname "$0")/config.sh"
source "$(dirname "$0")/logging.sh"

# --------------------------------------------------------------------------------------------
# run.sh - run script that builds a shadowjar and executes it
# --------------------------------------------------------------------------------------------
# Author: Matej Stastny
# Date: 2025-07-15 (YYYY-MM-DD)
# License: MIT
# Link: https://github.com/matejstastny/dotfiles
# --------------------------------------------------------------------------------------------

# Helpers ------------------------------------------------------------------------------------

usage() {
    echo "Usage: $0 <client|server|editor> [OPTIONS]"
    echo "Options:"
    echo " -h, --help            Display this help message"
    echo " -r, --rebuild         Deletes all files in the server temp directory."
    exit 0
}

# Flags --------------------------------------------------------------------------------------

TARGET_MODULE=""
REBUILD="false"
while [[ "$#" -gt 0 ]]; do
    case "$1" in
    -h | --help)
        usage
        ;;
    -r | --rebuld)
        REBUILD="true"
        ;;
    client)
        TARGET_MODULE="client"
        ;;
    server)
        TARGET_MODULE="server"
        ;;
    editor)
        TARGET_MODULE="editor"
        ;;
    *)
        log error "Unknown argument: $1"
        ;;
    esac
    shift
done

if [[ -z "$TARGET_MODULE" ]]; then
    log error "No module specified! Use 'client', 'server' or 'editor'."
fi

if [[ "$TARGET_MODULE" != "server" && "$REBUILD" == "true" ]]; then
    log warn "The -r flag will not have any effect when not running the server."
fi

log info "Building module: $TARGET_MODULE"

# Dependencies -------------------------------------------------------------------------------

JAVA_PATH=$(command -v java)
if [[ -z "$JAVA_PATH" ]]; then
    log error "Java is not installed or not in PATH."
fi

JAVA_VERSION=$("$JAVA_PATH" -version 2>&1 | awk -F'"' '/version/ {print $2}' | awk -F'.' '{print ($1 == "1" ? $2 : $1)}')

if [[ "$JAVA_VERSION" -ne "$JAVA_VER" ]]; then
    log error "Incorrect Java version ($JAVA_VERSION). Required: $JAVA_VER"
fi

log success "Using Java at $JAVA_PATH (Version $JAVA_VERSION)"

# Shadowjar ----------------------------------------------------------------------------------

log info "Running Gradle shadowJar..."
if [[ -f "$PROJECT_ROOT/app/gradlew" ]]; then
    (cd "$PROJECT_ROOT/app" && ./gradlew shadowJar --warning-mode none || {
        log error "Gradle shadowJar failed."
        exit 1
    })
else
    (cd "$PROJECT_ROOT/app" && gradle shadowJar --warning-mode none || {
        log error "Gradle shadowJar failed."
        exit 1
    })
fi
log success "shadowJar build completed."

# Jar ----------------------------------------------------------------------------------------

JAR_DIR="$PROJECT_ROOT/app/shadowjar"
JAR_FILE=$(find "$JAR_DIR" -maxdepth 1 -type f -name "flaggi-${TARGET_MODULE}*.jar" | head -n 1)

if [[ -z "$JAR_FILE" ]]; then
    log error "No matching JAR found in $JAR_DIR (pattern: flaggi-${TARGET_MODULE}*.jar)"
    exit 1
fi

# Server dir ---------------------------------------------------------------------------------

if [[ "$TARGET_MODULE" == "server" ]]; then
    log info "Setting up server temp dir..."
    SERVER_DIR="$PROJECT_ROOT/$SERVER_DIR"

    if [[ "$REBUILD" == "true" ]]; then
        if [[ -d "$SERVER_DIR" ]]; then
            log info "Rebuilding: Removing existing server directory..."
            rm -rf "$SERVER_DIR"
        else
            log info "Server directory doesn't exist. No need to remove."
        fi
    fi

    log info "Creating server directory..."
    mkdir -p "$SERVER_DIR"
    if [[ -f "$JAR_FILE" ]]; then
        log info "Moving server JAR file..."
        mv "$JAR_FILE" "$SERVER_DIR"
        JAR_FILE="$SERVER_DIR/$(basename "$JAR_FILE")"
        log success "Server JAR moved successfully to $SERVER_DIR"
    else
        log error "JAR file does not exist: $JAR_FILE"
        exit 1
    fi
fi

# Execute ------------------------------------------------------------------------------------

printf "\e[90m%$(tput cols)s\e[0m" '' | tr ' ' -
java -jar "$JAR_FILE"
