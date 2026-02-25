#!/usr/bin/env bash

# --------------------------------------------------------------------------------------------
# run-wrapper.sh - Wrapper around run.sh that supports --skip-build
# Drop this into the repo root alongside run.sh
# --------------------------------------------------------------------------------------------

set -e

source "$(dirname "$0")/lib/config.sh"
source "$(dirname "$0")/lib/shared.sh"
source "$(dirname "$0")/lib/logging.sh"

usage() {
    echo "Usage: $0 <client|server|editor> [--skip-build] [-r|--rebuild] [-h|--help]"
    exit 0
}

TARGET_MODULE=""
REBUILD="false"
SKIP_BUILD="false"

while [[ "$#" -gt 0 ]]; do
    case "$1" in
    client | server | editor) TARGET_MODULE="$1" ;;
    -h | --help) usage ;;
    -r | --rebuild) REBUILD="true" ;;
    --skip-build) SKIP_BUILD="true" ;;
    *)
        echo "Unknown argument: $1"
        exit 1
        ;;
    esac
    shift
done

[[ -z "$TARGET_MODULE" ]] && {
    echo "No module specified."
    exit 1
}
[[ "$TARGET_MODULE" != "server" && "$REBUILD" == "true" ]] && log warn "-r/--rebuild only applies to the server module, ignoring."

log info "Target: $TARGET_MODULE"
check_java_ver "$JAVA_VERSION" || {
    echo "Java version check failed."
    exit 1
}

if [[ "$SKIP_BUILD" == "false" ]]; then
    run_shadowjar || {
        echo "shadowjar build failed."
        exit 1
    }
fi

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
