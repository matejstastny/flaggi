#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# package.sh - Build a native installer using jpackage + jlink
#
# Usage:
#   ./package.sh [client] [OPTIONS]
#
# Options:
#   --version <ver>   Override the app version           (default: from gradle.properties)
#   --skip-build      Skip the Gradle build step
#   --help            Show this message
#
# Requirements:
#   All platforms : JDK 21+
#   macOS only    : create-dmg  (brew install create-dmg)
#   Windows only  : WiX 3.x  (jpackage does NOT support WiX 4+)
#                   Set JAVA_HOME and WIX as system environment variables.
# -----------------------------------------------------------------------------

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/lib/logging.sh"

PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# =============================================================================
# Config
# =============================================================================

# App
APP_NAME="Flaggi"
APP_VERSION=$(grep '^version=' "$PROJECT_ROOT/gradle.properties" | cut -d= -f2)
JAVA_MIN_VERSION=21

# Assets
MAC_ICON="$PROJECT_ROOT/assets/icons/flaggi.icns"
DMG_BACKGROUND="$PROJECT_ROOT/assets/banners/dmg-background.png"
WIN_ICON="$PROJECT_ROOT/assets/icons/win.ico"

# Directories
DIR_SHADOWJAR="$PROJECT_ROOT/shadowjar"
DIR_DIST="$PROJECT_ROOT/dist"
DIR_JRE="$PROJECT_ROOT/build/jre"
DIR_APP_IMAGE="$PROJECT_ROOT/build/app-image"

# jpackage extra flags
MAC_JPACKAGE_OPTS=""
WIN_JPACKAGE_OPTS="--win-per-user-install --win-menu --win-shortcut"

# =============================================================================
# Helpers
# =============================================================================

print_help() {
    awk 'NR==1{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "$0"
    exit 0
}

require_cmd() {
    local cmd="$1" hint="${2:-}"
    if ! command -v "$cmd" &>/dev/null; then
        log_err "'$cmd' not found.${hint:+  $hint}"
        exit 1
    fi
    log_inf "Found: ${_BOLD}$cmd${_R} → $(command -v "$cmd")"
}

require_file() {
    local file="$1" label="${2:-}"
    [[ -f "$file" ]] || die "Asset not found${label:+ ($label)}: $file"
}

to_unix_path() {
    local p="$1"
    if command -v cygpath >/dev/null 2>&1; then
        cygpath -u "$p"
    else
        local drive="${p:0:1}"
        drive="${drive,,}"
        p="/$drive${p:2}"
        echo "${p//\\//}"
    fi
}

# =============================================================================
# Argument parsing
# =============================================================================

TARGET_MODULE="client"
SKIP_BUILD=false

while [[ $# -gt 0 ]]; do
    case "$1" in
    client)
        TARGET_MODULE="$1"
        shift
        ;;
    --version)
        APP_VERSION="$2"
        shift 2
        ;;
    --skip-build)
        SKIP_BUILD=true
        shift
        ;;
    --help | -h) print_help ;;
    *) die "Unknown option: $1" ;;
    esac
done

# =============================================================================
# Environment setup
# =============================================================================

OS="$(uname -s)"

case "$OS" in
MINGW* | MSYS* | CYGWIN*)
    [[ -z "${JAVA_HOME:-}" ]] && die "JAVA_HOME is not set. Point it to your JDK $JAVA_MIN_VERSION+ installation directory"
    JAVA_HOME="$(to_unix_path "$JAVA_HOME")"
    export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
    log_inf "JAVA_HOME → $JAVA_HOME"

    [[ -z "${WIX:-}" ]] && {
        log_err "WIX is not set. jpackage requires WiX 3 (WiX 4+ is NOT supported)"
        log_inf "1. Download WiX 3: https://github.com/wixtoolset/wix3/releases/latest"
        log_inf "2. Install it, then add a system variable:  WIX=<install directory>"
        exit 1
    }
    WIX="$(to_unix_path "$WIX")"
    export WIX PATH="$WIX/bin:$PATH"
    log_inf "WIX       → $WIX"
    ;;
*)
    if [[ -z "${JAVA_HOME:-}" ]]; then
        if command -v /usr/libexec/java_home &>/dev/null; then
            JAVA_HOME="$(/usr/libexec/java_home)"
        else
            JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
        fi
        log_inf "Auto-detected JAVA_HOME → $JAVA_HOME"
    else
        log_inf "JAVA_HOME → $JAVA_HOME"
    fi
    export JAVA_HOME
    ;;
esac

# =============================================================================
# Prerequisite checks
# =============================================================================

log_inf "Checking prerequisites..."

require_cmd java
require_cmd jdeps "Ensure JAVA_HOME points to a full JDK, not a JRE."
require_cmd jlink "Ensure JAVA_HOME points to a full JDK, not a JRE."
require_cmd jpackage "Ensure JAVA_HOME points to a full JDK, not a JRE."

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print $1}')
[[ "$JAVA_VERSION" -ge "$JAVA_MIN_VERSION" ]] ||
    die "Java $JAVA_VERSION detected - Java $JAVA_MIN_VERSION+ is required."
log_inf "Java version: ${_BOLD}$JAVA_VERSION${_R}"

case "$OS" in
Darwin*)
    require_cmd create-dmg "Install with: brew install create-dmg"
    require_file "$MAC_ICON" "MAC_ICON"
    require_file "$DMG_BACKGROUND" "DMG_BACKGROUND"
    ;;
MINGW* | MSYS* | CYGWIN*)
    require_cmd candle.exe "Ensure WIX points to the root WiX install directory, not the bin subfolder."
    require_cmd light.exe "Ensure WIX points to the root WiX install directory, not the bin subfolder."
    require_file "$WIN_ICON" "WIN_ICON"
    ;;
esac

# =============================================================================
# Build
# =============================================================================

if [[ "$SKIP_BUILD" == false ]]; then
    log_inf "Building shadowJar..."
    (cd "$PROJECT_ROOT" && ./gradlew shadowJar --quiet) || die "shadowJar build failed."
    log_ok "shadowJar built."
fi

JAR_FILE="$DIR_SHADOWJAR/flaggi-${TARGET_MODULE}-${APP_VERSION}.jar"
[[ -f "$JAR_FILE" ]] || die "JAR not found: $JAR_FILE"

# =============================================================================
# Custom JRE (jlink)
# =============================================================================

log_inf "Resolving required modules..."
MODULES=$(jdeps --multi-release "$JAVA_MIN_VERSION" --print-module-deps \
    --ignore-missing-deps "$JAR_FILE")
log_inf "Modules: $MODULES"

log_inf "Building custom JRE..."
rm -rf "$DIR_JRE"
jlink \
    --add-modules "$MODULES" \
    --output "$DIR_JRE" \
    --strip-debug \
    --no-header-files \
    --no-man-pages
log_ok "Custom JRE → $DIR_JRE"

# =============================================================================
# Package
# =============================================================================

mkdir -p "$DIR_DIST"

case "$OS" in
Darwin*)
    rm -rf "$DIR_APP_IMAGE" && mkdir -p "$DIR_APP_IMAGE"
    # shellcheck disable=SC2086
    jpackage \
        --input "$DIR_SHADOWJAR" \
        --main-jar "flaggi-${TARGET_MODULE}-${APP_VERSION}.jar" \
        --name "$APP_NAME" \
        --app-version "$APP_VERSION" \
        --type app-image \
        --dest "$DIR_APP_IMAGE" \
        --runtime-image "$DIR_JRE" \
        --icon "$MAC_ICON" \
        $MAC_JPACKAGE_OPTS
    log_ok ".app bundle built → $DIR_APP_IMAGE/$APP_NAME.app"

    DMG_PATH="$DIR_DIST/${APP_NAME}-${APP_VERSION}.dmg"
    rm -f "$DMG_PATH"
    _dmg_err=$(mktemp)
    create-dmg \
        --volname "$APP_NAME" \
        --volicon "$MAC_ICON" \
        --background "$DMG_BACKGROUND" \
        --window-pos 200 120 \
        --window-size 660 400 \
        --icon-size 100 \
        --icon "${APP_NAME}.app" 165 190 \
        --hide-extension "${APP_NAME}.app" \
        --app-drop-link 495 190 \
        "$DMG_PATH" \
        "$DIR_APP_IMAGE" \
        >/dev/null 2>"$_dmg_err" || {
        log_err "create-dmg failed:"
        cat "$_dmg_err" >&2
        rm -f "$_dmg_err"
        exit 1
    }
    rm -f "$_dmg_err"
    log_ok "DMG → $DMG_PATH"
    ;;

MINGW* | MSYS* | CYGWIN*)
    log_inf "Packaging Windows EXE..."
    # shellcheck disable=SC2086
    jpackage \
        --input "$DIR_SHADOWJAR" \
        --main-jar "flaggi-${TARGET_MODULE}-${APP_VERSION}.jar" \
        --name "$APP_NAME" \
        --app-version "$APP_VERSION" \
        --type exe \
        --dest "$DIR_DIST" \
        --runtime-image "$DIR_JRE" \
        --icon "$WIN_ICON" \
        $WIN_JPACKAGE_OPTS
    log_ok "EXE → $DIR_DIST"
    ;;

*)
    die "Unsupported OS: $OS"
    ;;
esac

log_ok "Done. Packaged ${_BOLD}$APP_NAME $APP_VERSION${_R}."
