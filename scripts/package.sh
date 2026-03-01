#!/usr/bin/env bash

# --------------------------------------------------------------------------------------------
# package.sh - Packages the application into a .dmg installer
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
    echo "Usage: $0 <client|editor> [-h|--help] [-d|--debug]"
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
    *) die "Unknown argument: $1" ;;
    esac
    shift
done

[[ -z "$TARGET_MODULE" ]] && die "No module specified. Usage: client|editor"
[[ "$DEBUG" -eq 1 ]] && log info "Debug enabled."

log info "Target: $TARGET_MODULE"

# Dependencies -------------------------------------------------------------------------------

REQUIRED_COMMANDS=("realpath" "java" "jlink" "jpackage" "create-dmg")
for cmd in "${REQUIRED_COMMANDS[@]}"; do
    command -v "$cmd" &>/dev/null || die "Missing required command: $cmd"
done

# Shadowjar ----------------------------------------------------------------------------------

check_java_ver "$JAVA_VERSION" || die "Java version check failed."

APP_VERSION=$(get_project_ver)
log info "Project version: $APP_VERSION"

run_shadowjar || die "shadowJar build failed."
JAR_FILE=$(get_shadowjar_path)

# Cleanup ------------------------------------------------------------------------------------

cleanup() {
    [[ -d "$DIR_APP" ]] && rm -rf "$DIR_APP"
    [[ -d "$DIR_JRE" ]] && rm -rf "$DIR_JRE"
    [[ -f "$VANILLA_DMG_FILE" ]] && rm -f "$VANILLA_DMG_FILE"
}
trap cleanup EXIT

# JRE ----------------------------------------------------------------------------------------

DIR_JRE="${DIR_BUILD}/jre"
VANILLA_DMG_FILE="$DIR_BUILD/mac/$APP_NAME-$APP_VERSION.dmg"
rm -rf "$DIR_JRE"

log info "Scanning required modules..."
SCANNED_MODULES=$(scan_required_modules "$JAR_FILE") || die "Module scan failed."
log info "Modules: $SCANNED_MODULES"

log info "Building diet JRE..."
run_quiet jlink \
    --output "$DIR_JRE" \
    --add-modules "$SCANNED_MODULES" \
    --strip-debug \
    --no-man-pages \
    --no-header-files
[[ -d "$DIR_JRE" ]] || die "jlink failed to produce a JRE."
log done "Diet JRE built."

# JPackage -----------------------------------------------------------------------------------

log info "Packaging application..."
mkdir -p "$DIR_BUILD/mac"
run_quiet jpackage \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --input "$(dirname "$JAR_FILE")" \
    --main-jar "$(basename "$JAR_FILE")" \
    --runtime-image "$DIR_JRE" \
    --icon "$MAC_ICON" \
    --type dmg \
    --dest "$DIR_BUILD/mac"
[[ -f "$VANILLA_DMG_FILE" ]] || die "jpackage failed, expected: $VANILLA_DMG_FILE"
log done "Application packaged."

# App extraction -----------------------------------------------------------------------------

mkdir -p "$DIR_APP"
MOUNT_POINT=$(mktemp -d)

log info "Mounting DMG..."
hdiutil attach "$VANILLA_DMG_FILE" -nobrowse -mountpoint "$MOUNT_POINT" ||
    die "Failed to mount $VANILLA_DMG_FILE"

APP_SRC=$(find "$MOUNT_POINT" -maxdepth 1 -name "*.app" -print -quit)
if [[ -z "$APP_SRC" ]]; then
    hdiutil detach "$MOUNT_POINT" &>/dev/null
    die "No .app bundle found in mounted DMG."
fi

cp -pPR "$APP_SRC" "$DIR_APP"
run_quiet hdiutil detach "$MOUNT_POINT"
log done "App extracted."

# DMG Installer ------------------------------------------------------------------------------

log info "Creating DMG installer..."
mkdir -p "$DIR_DIST"
rm -f "$DIR_DIST/$APP_NAME-$APP_VERSION.dmg"

run_quiet create-dmg \
    --volname "$APP_NAME Installer" \
    --background "$DMG_BACKGROUND" \
    --window-pos 200 120 \
    --window-size 600 420 \
    --icon-size 100 \
    --icon "$APP_NAME.app" 180 210 \
    --app-drop-link 430 210 \
    --format UDBZ \
    "$DIR_DIST/$APP_NAME-$APP_VERSION.dmg" \
    "$DIR_APP"

find "$DIR_DIST" -maxdepth 1 -type f -name "*.dmg" ! -name "$APP_NAME-$APP_VERSION.dmg" -delete

log done "DMG installer created: $DIR_DIST/$APP_NAME-$APP_VERSION.dmg"
