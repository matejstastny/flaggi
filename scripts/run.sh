#!/usr/bin/env bash

# --------------------------------------------------------------------------------------------
# run.sh - Builds a shadowjar and executes it
# --------------------------------------------------------------------------------------------
# Author: Matej Stastny
# Date: 2025-11-20 (YYYY-MM-DD)
# License: MIT
# Link: https://github.com/matejstastny/flaggi
# --------------------------------------------------------------------------------------------

set -e

source "$(dirname "$0")/lib/config.sh"
source "$(dirname "$0")/lib/shared.sh"
source "$(dirname "$0")/lib/logging.sh"

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
    *) die "Unknown argument: $1" ;;
    esac
    shift
done

[[ -z "$TARGET_MODULE" ]] && die "No module specified."
[[ "$TARGET_MODULE" != "server" && "$REBUILD" == "true" ]] && log warn "-r/--rebuild only applies to the server module, ignoring."

# Main ---------------------------------------------------------------------------------------

log info "Target: $TARGET_MODULE"

check_java_ver "$JAVA_VERSION" || die "Java version check failed."

run_shadowjar || die "shadowjar build failed."
JAR_FILE=$(get_shadowjar_path)

if [[ "$TARGET_MODULE" == "server" ]]; then
    if [[ "$REBUILD" == "true" && -d "$DIR_SERVER_TEMP" ]]; then
        log info "Rebuild requested, clearing $DIR_SERVER_TEMP."
        rm -rf "$DIR_SERVER_TEMP"
    fi
    mkdir -p "$DIR_SERVER_TEMP"
    mv "$JAR_FILE" "$DIR_SERVER_TEMP"
    JAR_FILE="$DIR_SERVER_TEMP/$(basename "$JAR_FILE")"
fi

log info "Launching $(basename "$JAR_FILE")..."
echo ""
echo "Flaggi --------------------------------------------"
exec java -jar "$JAR_FILE"
