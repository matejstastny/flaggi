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

while [[ $# -gt 0 ]]; do
    case "$1" in
    client | editor) TARGET_MODULE="$1" ;;
    -d | --debug) DEBUG=1 ;;
    -h | --help) usage ;;
    *)
        log error "Unknown argument: $1"
        exit 1
        ;;
    esac
    shift
done

((DEBUG)) && log info "Debug enabled"
[[ -z "$TARGET_MODULE" ]] && log error "Specify module: client|editor" && exit 1

log info "Building module: $TARGET_MODULE"

# Dependencies -------------------------------------------------------------------------------

REQUIRED_COMMANDS=("realpath" "java" "jlink" "jpackage" "grep")
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

cleanup() { [[ -d "$DIR_APP" ]] && rm -rf "$DIR_APP"; }
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
log info "Extracting app file"
MOUNT=$(hdiutil attach "$VANILLA_DMG_FILE" -nobrowse)
VOL=$(echo "$MOUNT" | grep -o '/Volumes/.*') || {
    log error "Mount failed"
    exit 1
}

APP_SRC=$(find "$VOL" -maxdepth 1 -name "*.app" -print -quit)
[[ -z "$APP_SRC" ]] && {
    log error "No app found"
    hdiutil detach "$VOL"
    exit 1
}

cp -pPR "$APP_SRC" "$DIR_APP"
run_quiet hdiutil detach "$VOL"

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
# Delete trash dmg files create-dmg creates
find "$DIR_DIST" -maxdepth 1 -type f -name "*.$APP_NAME-$APP_VERSION.dmg" ! -name "$APP_NAME-$APP_VERSION.dmg" -delete
log celebrate "DMG installer created"
