#!/usr/bin/env bash

set -e
source "$(dirname "$0")/config.sh"
source "$(dirname "$0")/logging.sh"

# --------------------------------------------------------------------------------------------
# run.sh - run script that builds a shadowjar and executes it
# --------------------------------------------------------------------------------------------
# Author: Matej Stastny
# Date: 2025-11-20 (YYYY-MM-DD)
# License: MIT
# Link: https://github.com/matejstastny/flaggi
# --------------------------------------------------------------------------------------------

# Helpers ------------------------------------------------------------------------------------

usage() {
    echo "Usage: $0 <client|server|editor> [-h|--help] [-r|--rebuild]"
    exit 0
}

# Flags --------------------------------------------------------------------------------------

TARGET_MODULE=""
REBUILD="false"

while [[ "$#" -gt 0 ]]; do
    case "$1" in
    client | server | editor) TARGET_MODULE="$1" ;;
    -h | --help) usage ;;
    -r | --rebuild) REBUILD="true" ;;
    *) log error "Unknown argument: $1" ;;
    esac
    shift
done

[[ -z "$TARGET_MODULE" ]] && log error "No module specified."
[[ "$TARGET_MODULE" != "server" && "$REBUILD" == "true" ]] && log warn "-r has no effect for this module."

log info "Building module: $TARGET_MODULE"

# Dependencies -------------------------------------------------------------------------------

JAVA_PATH=$(command -v java) || log error "Java not found."
JAVA_VERSION=$("$JAVA_PATH" -version 2>&1 | awk -F'"' '/version/ {print $2}' | awk -F'.' '{print ($1=="1"?$2:$1)}')
[[ "$JAVA_VERSION" -ne "$JAVA_VER" ]] && log error "Java $JAVA_VER required."

log success "Using Java $JAVA_VERSION at $JAVA_PATH"

# Shadowjar ----------------------------------------------------------------------------------

log info "Running shadowJar..."
GRADLE_CMD="$PROJECT_ROOT/gradlew"
[[ -f "$GRADLE_CMD" ]] || GRADLE_CMD="gradle"

(cd "$PROJECT_ROOT" && "$GRADLE_CMD" shadowJar --warning-mode none) ||
    log error "shadowJar failed."

log success "shadowJar completed."

# Jar ----------------------------------------------------------------------------------------

JAR_DIR="$PROJECT_ROOT/shadowjar"
JAR_FILE=$(find "$JAR_DIR" -maxdepth 1 -type f -name "flaggi-${TARGET_MODULE}*.jar" | head -n 1) ||
    true

[[ -z "$JAR_FILE" ]] && log error "No JAR found for pattern: flaggi-${TARGET_MODULE}*.jar"

# Server dir ---------------------------------------------------------------------------------

if [[ "$TARGET_MODULE" == "server" ]]; then
    SERVER_DIR="$PROJECT_ROOT/$SERVER_DIR"
    [[ "$REBUILD" == "true" && -d "$SERVER_DIR" ]] && rm -rf "$SERVER_DIR"
    mkdir -p "$SERVER_DIR"
    mv "$JAR_FILE" "$SERVER_DIR"
    JAR_FILE="$SERVER_DIR/$(basename "$JAR_FILE")"
fi

# Execute ------------------------------------------------------------------------------------

printf "\e[90m%$(tput cols)s\e[0m" '' | tr ' ' -
exec java -jar "$JAR_FILE"
