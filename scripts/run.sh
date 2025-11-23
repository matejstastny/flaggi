#!/usr/bin/env bash

# --------------------------------------------------------------------------------------------
# run.sh - run script that builds a shadowjar and executes it
# --------------------------------------------------------------------------------------------
# Author: Matej Stastny
# Date: 2025-11-20 (YYYY-MM-DD)
# License: MIT
# Link: https://github.com/matejstastny/flaggi
# --------------------------------------------------------------------------------------------

set -e
source "$(dirname "$0")/config.sh"
source "$(dirname "$0")/shared.sh"
source "$(dirname "$0")/logging.sh"

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

[[ -z "$TARGET_MODULE" ]] && log error "No module specified." && exit 1
[[ "$TARGET_MODULE" != "server" && "$REBUILD" == "true" ]] && log warn "-r has no effect for this module."

log info "Building module: $TARGET_MODULE"

# Main ---------------------------------------------------------------------------------------

check_java_ver "$JAVA_VERSION" || exit 1
APP_VERSION=$(get_project_ver)
run_shadowjar || exit 1
JAR_FILE=$(get_shadowjar_path)

# Setup temp server environment
if [[ "$TARGET_MODULE" == "server" ]]; then
    SERVER_DIR="$PROJECT_ROOT/$SERVER_DIR"
    [[ "$REBUILD" == "true" && -d "$SERVER_DIR" ]] && rm -rf "$SERVER_DIR"
    mkdir -p "$SERVER_DIR"
    mv "$JAR_FILE" "$SERVER_DIR"
    JAR_FILE="$SERVER_DIR/$(basename "$JAR_FILE")"
fi

# Execute
echo ""
echo "Flaggi --------------------------------------------"
exec java -jar "$JAR_FILE"
