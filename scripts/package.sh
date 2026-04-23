#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# package.sh - Build a native installer using jpackage + jlink
#
# Flags:
#   --version <ver>   Override app version  (default: from gradle.properties)
#   --skip-build      Skip the Gradle shadowJar build
#   --help            Show this message
# ---------------------------------------------------------------------------

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/lib/logging.sh"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Config --------------------------------------------------------------------

APP_NAME="Flaggi"
APP_VERSION="$(grep '^version=' "$PROJECT_ROOT/gradle.properties" | cut -d= -f2)"
JAR_FILE="$PROJECT_ROOT/client/build/libs/flaggi-client.jar"
DIST_DIR="$PROJECT_ROOT/dist"
JRE_DIR="$PROJECT_ROOT/build/jre"
APP_IMAGE_DIR="$PROJECT_ROOT/build/app-image"

MAC_ICON="$PROJECT_ROOT/assets/icons/flaggi.icns"
DMG_BACKGROUND="$PROJECT_ROOT/assets/banners/dmg-background.png"
WIN_ICON="$PROJECT_ROOT/assets/icons/win.ico"
LINUX_ICON="$PROJECT_ROOT/assets/icons/console_icon.png"

SKIP_BUILD=false

# Argument parsing ----------------------------------------------------------

print_help() {
    awk 'NR==1{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "$0"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
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

# Environment ---------------------------------------------------------------

OS="$(uname -s)"

if [[ "$OS" == MINGW* || "$OS" == MSYS* || "$OS" == CYGWIN* ]]; then
    [[ -z "${JAVA_HOME:-}" ]] && die "JAVA_HOME is not set. Point it to your JDK 21+ directory."

    to_unix_path() {
        command -v cygpath &>/dev/null && {
            cygpath -u "$1"
            return
        }
        local d="${1:0:1}"
        d="${d,,}"
        echo "/$d${1:2}" | tr '\\' '/'
    }

    JAVA_HOME="$(to_unix_path "$JAVA_HOME")"
    export JAVA_HOME
    export PATH="$JAVA_HOME/bin:$PATH"

    if [[ -n "${WIX:-}" ]]; then
        WIX="$(to_unix_path "$WIX")"
        export WIX
        export PATH="$WIX/bin:$PATH"
    fi
fi

# Prerequisites -------------------------------------------------------------

for cmd in java jdeps jlink jpackage; do
    command -v "$cmd" &>/dev/null || die "'$cmd' not found. Ensure JAVA_HOME points to a full JDK 21+."
    log_inf "Found: ${_BOLD}$cmd${_R} → $(command -v "$cmd")"
done

JAVA_VER="$(java -version 2>&1 | awk -F'"' '/version/{print $2}' | cut -d. -f1)"
[[ "$JAVA_VER" -ge 21 ]] || die "Java $JAVA_VER detected - Java 21+ required."

case "$OS" in
Darwin*)
    command -v create-dmg &>/dev/null || die "'create-dmg' not found. Install with: brew install create-dmg"
    [[ -f "$MAC_ICON" ]] || die "Missing asset: $MAC_ICON"
    [[ -f "$DMG_BACKGROUND" ]] || die "Missing asset: $DMG_BACKGROUND"
    ;;
MINGW* | MSYS* | CYGWIN*)
    command -v candle.exe &>/dev/null || die "WiX 3 'candle.exe' not on PATH. Install WiX 3 and add its bin dir to PATH."
    command -v light.exe &>/dev/null || die "WiX 3 'light.exe' not on PATH."
    [[ -f "$WIN_ICON" ]] || die "Missing asset: $WIN_ICON"
    ;;
Linux*)
    [[ -f "$LINUX_ICON" ]] || die "Missing asset: $LINUX_ICON"
    ;;
esac

# Build ---------------------------------------------------------------------

if [[ "$SKIP_BUILD" == false ]]; then
    log_inf "Building shadowJar..."
    (cd "$PROJECT_ROOT" && ./gradlew :client:shadowJar --no-daemon --quiet) || die "shadowJar build failed."
    log_ok "shadowJar built."
fi

[[ -f "$JAR_FILE" ]] || die "JAR not found: $JAR_FILE"

# Custom JRE via jlink ------------------------------------------------------

log_inf "Resolving modules..."
MODULES="$(jdeps --multi-release 21 --print-module-deps --ignore-missing-deps "$JAR_FILE")"
log_inf "Modules: $MODULES"

log_inf "Building custom JRE..."
rm -rf "$JRE_DIR"
jlink \
    --add-modules "$MODULES" \
    --output "$JRE_DIR" \
    --strip-debug \
    --no-header-files \
    --no-man-pages
log_ok "JRE → $JRE_DIR"

# Package -------------------------------------------------------------------

mkdir -p "$DIST_DIR"

JPACKAGE_BASE=(
    --input "$(dirname "$JAR_FILE")"
    --main-jar "flaggi-client.jar"
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --runtime-image "$JRE_DIR"
)

case "$OS" in
Darwin*)
    rm -rf "$APP_IMAGE_DIR" && mkdir -p "$APP_IMAGE_DIR"
    jpackage "${JPACKAGE_BASE[@]}" \
        --type app-image \
        --dest "$APP_IMAGE_DIR" \
        --icon "$MAC_ICON"
    log_ok ".app → $APP_IMAGE_DIR/$APP_NAME.app"

    OUT_FILE="$DIST_DIR/${APP_NAME}-${APP_VERSION}-macos.dmg"
    rm -f "$OUT_FILE"
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
        "$OUT_FILE" \
        "$APP_IMAGE_DIR" \
        >/dev/null
    log_ok "DMG → $OUT_FILE"
    ;;

MINGW* | MSYS* | CYGWIN*)
    jpackage "${JPACKAGE_BASE[@]}" \
        --type exe \
        --dest "$DIST_DIR" \
        --icon "$WIN_ICON" \
        --win-per-user-install \
        --win-menu \
        --win-shortcut
    SRC="$(find "$DIST_DIR" -maxdepth 1 -name '*.exe' -print -quit)"
    OUT_FILE="$DIST_DIR/${APP_NAME}-${APP_VERSION}-windows.exe"
    mv "$SRC" "$OUT_FILE"
    log_ok "EXE → $OUT_FILE"
    ;;

Linux*)
    jpackage "${JPACKAGE_BASE[@]}" \
        --type app-image \
        --dest "$DIST_DIR" \
        --icon "$LINUX_ICON"
    tar -C "$DIST_DIR" -czf "$DIST_DIR/${APP_NAME}-${APP_VERSION}-linux.tar.gz" "$APP_NAME"
    OUT_FILE="$DIST_DIR/${APP_NAME}-${APP_VERSION}-linux.tar.gz"
    log_ok "tarball → $OUT_FILE"
    ;;

*)
    die "Unsupported OS: $OS"
    ;;
esac

# Done ----------------------------------------------------------------------

log_ok "Done. Packaged ${_BOLD}$APP_NAME $APP_VERSION${_R}."
[[ -n "${GITHUB_OUTPUT:-}" ]] && echo "output_file=$OUT_FILE" >>"$GITHUB_OUTPUT"
