#!/usr/bin/env bash
# --------------------------------------------------------------------------------------------
# run.sh — Builds a shadowJar and executes it
# Usage: run.sh <client|server|editor> [-h|--help] [-r|--rebuild] [--skip-build]
# --------------------------------------------------------------------------------------------

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/lib/logging.sh"

PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
JAVA_VERSION="21"
DIR_SHADOWJAR="$PROJECT_ROOT/shadowjar"
DIR_SERVER_TEMP="$PROJECT_ROOT/server-temp"

# Helpers ------------------------------------------------------------------------------------

usage() {
    echo "Usage: $0 <client|server|editor> [-h|--help] [-r|--rebuild] [--skip-build]"
    exit 0
}

check_java_ver() {
    local required="$1"
    local java_cmd
    java_cmd=$(command -v java || true)
    [[ -z "$java_cmd" ]] && die "Java not found."

    local version
    version=$("$java_cmd" -version 2>&1 | awk -F'"' '/version/ {print $2}' | awk -F'.' '{print ($1=="1"?$2:$1)}')
    [[ "$version" =~ ^[0-9]+$ ]] || die "Could not parse Java version: $version"
    [[ "$version" -eq "$required" ]] || die "Java $required required, found $version at $java_cmd."

    log_inf "Java $version at $java_cmd."
}

get_shadowjar_path() {
    local jar_file
    jar_file=$(find "$DIR_SHADOWJAR" -maxdepth 1 -type f -name "flaggi-${TARGET_MODULE}*.jar" | head -n 1)
    [[ -n "$jar_file" ]] || die "No JAR found matching: flaggi-${TARGET_MODULE}*.jar"
    echo "$jar_file"
}

# Flags --------------------------------------------------------------------------------------

TARGET_MODULE=""
REBUILD="false"
SKIP_BUILD="false"

while [[ "$#" -gt 0 ]]; do
    case "$1" in
    client | server | editor) TARGET_MODULE="$1" ;;
    -h | --help) usage ;;
    -r | --rebuild) REBUILD="true" ;;
    --skip-build) SKIP_BUILD="true" ;;
    *) die "Unknown argument: $1" ;;
    esac
    shift
done

[[ -z "$TARGET_MODULE" ]] && usage
[[ "$TARGET_MODULE" != "server" && "$REBUILD" == "true" ]] && log_wrn "-r/--rebuild only applies to the server module, ignoring."

# Main ---------------------------------------------------------------------------------------

log_inf "Target: $TARGET_MODULE"

check_java_ver "$JAVA_VERSION"

if [[ "$SKIP_BUILD" == "false" ]]; then
    log_inf "Building shadowJar..."
    (cd "$PROJECT_ROOT" && ./gradlew shadowJar --quiet) || die "shadowJar build failed."
    log_ok "shadowJar built."
fi

JAR_FILE=$(get_shadowjar_path)

if [[ "$TARGET_MODULE" == "server" ]]; then
    if [[ "$REBUILD" == "true" && -d "$DIR_SERVER_TEMP" ]]; then
        log_inf "Rebuild requested, clearing $DIR_SERVER_TEMP."
        rm -rf "$DIR_SERVER_TEMP"
    fi
    mkdir -p "$DIR_SERVER_TEMP"
    mv "$JAR_FILE" "$DIR_SERVER_TEMP"
    JAR_FILE="$DIR_SERVER_TEMP/$(basename "$JAR_FILE")"
fi

log_inf "Launching $(basename "$JAR_FILE")..."
echo ""
echo "Flaggi --------------------------------------------"
exec java -jar "$JAR_FILE"
