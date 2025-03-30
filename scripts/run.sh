#!/bin/bash

set -e

# ───────────────────────────────────────────────────────────────────────────────
# Run Script for "Flaggi"
# Buils a JAR using Gradle and then executes it.
# ───────────────────────────────────────────────────────────────────────────────

# ─── Configuration ─────────────────────────────────────────────────────────────

PROJECT_ROOT=".."               # Location of the root of the project relative to this script
JAVA_VER="8"                    # Java version needed to build
APP_NAME="Flaggi"               # Name of the app (In the dmg name)
SERVER_DIR="flaggi-server-temp" # Name of the server directory (needs to be moved, because the JAR generates other files)
JAR_TASK="shadowjar"            # Task name called on Gradle to build a fat JAR

# Prefix for JAR task output:
# MAKE SURE THE ABOVE CONFIGURED JAR_TASK OUTPUTS THE
# JAR OUTPUT PATH PREFIXED BY THE FOLLOWING STRING
JAR_TASK_OUTPUT_PREFIX="Shadow JAR has been created at:"

# ─── Helpers ───────────────────────────────────────────────────────────────────

usage() {
    echo "Usage: $0 <client|server|editor> [OPTIONS]"
    echo "Options:"
    echo " -h, --help            Display this help message"
    echo " -r, --rebuild         Deletes all files in the server temp directory."
    exit 0
}

log_info() { echo "$(date +'%Y-%m-%d %H:%M:%S') 📦  $1"; }
log_success() { echo "$(date +'%Y-%m-%d %H:%M:%S') ✅  $1"; }
log_error() {
    echo "$(date +'%Y-%m-%d %H:%M:%S') ❌  $1" >&2
    exit 1
}

# Ensure `realpath` exists, fallback if missing
if ! command -v realpath &>/dev/null; then
    log_info "realpath not found, using fallback method"
    PROJECT_ROOT="$(cd "$PROJECT_ROOT" && pwd)"
else
    PROJECT_ROOT=$(realpath "$PROJECT_ROOT")
fi

change_dirs() {
    if [ -d "$1" ]; then
        cd "$1" || log_error "Failed to navigate to $1. Exiting."
    else
        log_error "Directory $1 does not exist. Exiting."
    fi
}

check_file_exists() {
    local file_path="$1"
    if [[ -f "$file_path" ]]; then
        return 0
    else
        log_error "'$file_path' doesn't exist."
        return 1
    fi
}

# ─── Parse flags ───────────────────────────────────────────────────────────────

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
        log_error "Unknown argument: $1"
        ;;
    esac
    shift
done

if [[ -z "$TARGET_MODULE" ]]; then
    log_error "No module specified! Use 'client', 'server' or 'editor'."
fi

log_info "Building module: $TARGET_MODULE"

# ─── Dependencies ──────────────────────────────────────────────────────────────

JAVA_PATH=$(command -v java)
if [[ -z "$JAVA_PATH" ]]; then
    log_error "Java is not installed or not in PATH."
fi

JAVA_VERSION=$("$JAVA_PATH" -version 2>&1 | awk -F'"' '/version/ {print $2}' | awk -F'.' '{print ($1 == "1" ? $2 : $1)}')

if [[ "$JAVA_VERSION" -ne "$JAVA_VER" ]]; then
    log_error "Incorrect Java version ($JAVA_VERSION). Required: $JAVA_VER"
fi

log_success "Using Java at $JAVA_PATH (Version $JAVA_VERSION)"

# ─── JAR task ──────────────────────────────────────────────────────────────────

change_dirs "$PROJECT_ROOT/app"

log_info "Building Gradle JAR..."
check_file_exists "./gradlew"

GRADLE_OUTPUT=$(./gradlew :$TARGET_MODULE:$JAR_TASK --rerun-tasks 2>&1) || log_error "Gradle build failed."
JAR_FILE=$(echo "$GRADLE_OUTPUT" | awk -F': ' "/${JAR_TASK_OUTPUT_PREFIX}/ {print \$2}")
check_file_exists "$JAR_FILE"

log_success "Shadow JAR task finished"

# ─── JAR execution ─────────────────────────────────────────────────────────────

if [[ "$TARGET_MODULE" == "server" ]]; then
    log_info "Setting up server temporary directory..."
    SERVER_DIR="$PROJECT_ROOT/$SERVER_DIR"

    if [[ "$REBUILD" == "true" ]]; then
        if [[ -d "$SERVER_DIR" ]]; then
            log_info "Rebuilding: Removing existing server directory..."
            rm -rf "$SERVER_DIR"
        else
            log_info "Server directory doesn't exist. No need to remove."
        fi
    fi

    log_info "Creating server directory..."
    mkdir -p "$SERVER_DIR"
    if [[ -f "$JAR_FILE" ]]; then
        log_info "Moving server JAR file..."
        mv "$JAR_FILE" "$SERVER_DIR"
        JAR_FILE="$SERVER_DIR/$(basename "$JAR_FILE")"
        log_success "Server JAR moved successfully to $SERVER_DIR"
    else
        log_error "JAR file does not exist: $JAR_FILE"
        exit 1
    fi
fi

printf "%$(tput cols)s" '' | tr ' ' -
java -jar "$JAR_FILE"
