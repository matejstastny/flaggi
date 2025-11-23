#!/usr/bin/env bash

# --------------------------------------------------------------------------------------------
# package.sh - packaging script to .dmg and .exe archives
# --------------------------------------------------------------------------------------------
# Author: Matej Stastny
# Date: 2025-11-20 (YYYY-MM-DD)
# License: MIT
# Link: https://github.com/matejstastny/flaggi
# --------------------------------------------------------------------------------------------

source "$(dirname "$0")/config.sh"
source "$(dirname "$0")/shared.sh"
source "$(dirname "$0")/logging.sh"

# Helpers ------------------------------------------------------------------------------------

usage() {
    echo "Usage: $0 <client|editor> [OPTIONS]"
    echo "Options:"
    echo " -h, --help            Display this help message"
    echo " -d, --debug           Enabled verbose debug output"
    exit 0
}

# Flags --------------------------------------------------------------------------------------

TARGET_MODULE=""
DEBUG=0

while [[ "$#" -gt 0 ]]; do
    case "$1" in
    client | editor)
        TARGET_MODULE="$1"
        ;;
    -d | --debug) DEBUG=1 ;;
    -h | --help) usage ;;
    *)
        log error "Unknown argument: $1"
        exit 1
        ;;
    esac
    shift
done

((DEBUG == 1)) && log info "Debug logs enabled"

if [[ -z "$TARGET_MODULE" ]]; then
    log error "No module specified! Use 'client' or 'editor'"
    exit 1
fi

log info "Building module: $TARGET_MODULE"

# Dependencies -------------------------------------------------------------------------------

REQUIRED_COMMANDS=("realpath" "java" "jlink" "jpackage" "awk" "grep")
for cmd in "${REQUIRED_COMMANDS[@]}"; do
    if ! command -v "$cmd" &>/dev/null; then
        log error "Missing required command: $cmd"
        exit 1
    fi
done

# Shadowjar ----------------------------------------------------------------------------------

check_java_ver "$JAVA_VERSION" || exit 1
APP_VERSION=$(get_project_ver)
log info "Project version: $APP_VERSION"
run_shadowjar || exit 1
JAR_FILE=$(get_shadowjar_path)

# Cleanup ------------------------------------------------------------------------------------

cleanup() {
    log info "Cleaning up temporary files..."
    [[ -d "$DIR_APP" ]] && rm -rf "$DIR_APP"
    log info "Cleaned up"
}
trap cleanup EXIT

# JRE ----------------------------------------------------------------------------------------

DIR_JRE="${DIR_BUILD}/jre"
rm -rf "$DIR_JRE"

log info "Scanning required modules..."
SCANNED_MODULES=$(scan_required_modules "$JAR_FILE") || exit 1
log info "Included modules: $SCANNED_MODULES"

log info "Building diet JRE..."
run_quiet jlink --output "$DIR_JRE" --add-modules "$SCANNED_MODULES" --strip-debug --no-man-pages --no-header-files
[[ ! -d "$DIR_JRE" ]] && log error "JLink failed to create JRE" && exit 1
log success "Diet JRE built successfully"

# JPackage -----------------------------------------------------------------------------------

JPACKAGE_ARGS=(
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --input "$(dirname "$JAR_FILE")"
    --main-jar "$(basename "$JAR_FILE")"
    --runtime-image "$DIR_JRE"
)

log info "Packaging application..."
run_quiet jpackage "${JPACKAGE_ARGS[@]}" --icon "$MAC_ICON" --type dmg --dest "$DIR_BUILD/mac"
VANILLA_DMG_FILE="$DIR_BUILD/mac/$APP_NAME-$APP_VERSION.dmg"
[[ ! -f "$VANILLA_DMG_FILE" ]] && log error "JPackage failed to package app at $VANILLA_DMG_FILE" && exit 1
log success "Application packaged"

# App package --------------------------------------------------------------------------------

mkdir -p "$DIR_APP"
MOUNT_INFO=$(hdiutil attach "$VANILLA_DMG_FILE" -nobrowse)
DMG_VOLUME=$(echo "$MOUNT_INFO" | grep -o '/Volumes/.*')

if [ -z "$DMG_VOLUME" ]; then
    log error "Failed to mount DMG or find volume"
    exit 1
fi

log info "Volume: $DMG_VOLUME"

APP_PATH=$(find "$DMG_VOLUME" -maxdepth 1 -name "*.app" -print -quit)

if [ -z "$APP_PATH" ]; then
    log error "No .app found in DMG"
    run_quiet hdiutil detach "$DMG_VOLUME"
    exit 1
fi

cp -pPR "$APP_PATH" "$DIR_APP"

if [ $? -eq 0 ]; then
    log success "Successfully copied $APP_PATH to $DIR_APP"
else
    log error "Failed to copy $APP_PATH"
    run_quiet hdiutil detach "$DMG_VOLUME"
    exit 1
fi

run_quiet run_quiet hdiutil detach "$DMG_VOLUME"

if [ $? -eq 0 ]; then
    log info "Unmounted $DMG_VOLUME"
else
    log warn "Failed to unmount $DMG_VOLUME"
fi

# DMG Installer ------------------------------------------------------------------------------

DMG_ARGS=(
    --volname "$APP_NAME Installer"
    --background "$DMG_BACKGROUND"
    --window-pos 200 120
    --window-size 600 420
    --icon-size 100
    --icon "$APP_NAME.app" 180 210
    --app-drop-link 430 210
    --format UDBZ
    "$DIR_DIST/$APP_NAME-$APP_VERSION.dmg"
    "$DIR_APP"
)

log info "Creating DMG installer..."
mkdir -p "$DIR_DIST"
rm -f "$DIR_DIST/$APP_NAME-$APP_VERSION.dmg"
run_quiet create-dmg "${DMG_ARGS[@]}"
log celebrate "DMG installer created"
