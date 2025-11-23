#!/bin/bash

set -e

# ───────────────────────────────────────────────────────────────────────────────
# Packaging Script for "Flaggi"
# Packages Gradle project using Shadowjar, then creates an app package using
# jpackage with jlink and changes the background using create-dmg if building
# for MacOS
# ───────────────────────────────────────────────────────────────────────────────

# ─── Configuration ─────────────────────────────────────────────────────────────

DEBUG=0                              # Set to 1 to enable verbose logs
PROJECT_ROOT=".."                    # Location of the root of the project relative to this script
JAVA_VER="8"                         # Java version needed to build
APP_NAME="Flaggi"                    # Name of the app (In the dmg name)
APP_VERSION="1.0.0"                  # Version of the app (In the dmg name)
JAR_TASK="shadowjar"                 # Task name called on Gradle to build a fat JAR
JRE_MODULES="java.base,java.desktop" # Modules included in the JRE

# Prefix for JAR task output:
# MAKE SURE GRADLE OUTPUTS THE PATH PREFIXED BY THE FOLLOWING STRING
JAR_TASK_OUTPUT_PREFIX="Shadow JAR has been created at:"

# The following will be located in ROOT
MAC_ICON="assets/icons/mac.icns"
WIN_ICON="assets/icons/win.ico"
BACKGROUND_IMG="assets/banners/dmg-background.png"

# The following dirs will be located in ROOT/app/build
TEMP_DIR="temp"   # Name of the temporary directory (will be deleted)
OUTPUT_DIR="apps" # Name of the output directory the '.exe' and '.dmg' files will be exported to

# ─── Platform ──────────────────────────────────────────────────────────────────

OS="$(uname)"
case "$OS" in
Darwin)
    PLATFORM="mac"
    ;;
Linux)
    PLATFORM="linux"
    ;;
MINGW* | MSYS*)
    PLATFORM="windows"
    ;;
*)
    echo "❌ Unsupported OS: $OS"
    exit 1
    ;;
esac

# ─── Ensure required commands exist before proceeding ───────────────────────────

REQUIRED_COMMANDS=("realpath" "java" "jlink" "jpackage" "awk" "grep")
for cmd in "${REQUIRED_COMMANDS[@]}"; do
    if ! command -v "$cmd" &>/dev/null; then
        log_error "Missing required command: $cmd"
    fi
done

# ─── Helpers ───────────────────────────────────────────────────────────────────

log_info() { echo "$(date +'%Y-%m-%d %H:%M:%S') 📦  $1"; }
log_success() { echo "$(date +'%Y-%m-%d %H:%M:%S') ✅  $1"; }
log_error() {
    echo "$(date +'%Y-%m-%d %H:%M:%S') ❌  $1" >&2
    exit 1
}

debug_log() {
    if [[ $DEBUG -eq 1 ]]; then
        echo "$(date +'%Y-%m-%d %H:%M:%S') 🐛 DEBUG: $1"
    fi
}

usage() {
    echo "Usage: $0 <client|editor> [OPTIONS]"
    echo "Options:"
    echo " -h, --help            Display this help message"
    echo " -d, --debug           Enabled verbose debug output"
    exit 0
}

run_cmd() {
    local cmd=("$@")
    debug_log "Running command: ${cmd[@]}"

    if ! command -v "${cmd[0]}" &>/dev/null; then
        log_error "Command not found: ${cmd[0]}"
        return 1
    fi

    local output
    output=$("${cmd[@]}" 2>&1)
    local status=$?

    debug_log "Command finished with status: $status"
    debug_log "Command output:\n$output"

    if [[ $status -ne 0 ]]; then
        log_error "Command failed: ${cmd[@]}\n$output"
    fi
}

# Ensure `realpath` exists, fallback if missing
if ! command -v realpath &>/dev/null; then
    log_info "realpath not found, using fallback method"
    PROJECT_ROOT="$(cd "$PROJECT_ROOT" && pwd)"
else
    PROJECT_ROOT=$(realpath "$PROJECT_ROOT")
fi

TEMP_DIR="$PROJECT_ROOT/app/build/$TEMP_DIR"
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
while [[ "$#" -gt 0 ]]; do
    case "$1" in
    -h | --help)
        usage
        ;;
    -d | --debug)
        DEBUG=1
        debug_log "Debug log enabled."
        ;;
    client)
        TARGET_MODULE="client"
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
    log_error "No module specified! Use 'client' or 'editor'."
fi

log_info "Building module: $TARGET_MODULE"

# ─── Dependencies ──────────────────────────────────────────────────────────────

log_info "Checking dependencies..."

check_file_exists "$PROJECT_ROOT/$MAC_ICON"
check_file_exists "$PROJECT_ROOT/$WIN_ICON"
check_file_exists "$PROJECT_ROOT/$BACKGROUND_IMG"

JAVA_PATH=$(command -v java)
if [[ -z "$JAVA_PATH" ]]; then
    log_error "Java is not installed or not in PATH."
fi

JAVA_VERSION=$("$JAVA_PATH" -version 2>&1 | awk -F'"' '/version/ {print $2}' | awk -F'.' '{print ($1 == "1" ? $2 : $1)}')

if [[ "$JAVA_VERSION" -ne "$JAVA_VER" ]]; then
    log_error "Incorrect Java version ($JAVA_VERSION). Required: $JAVA_VER"
fi

log_success "Using Java at $JAVA_PATH (Version $JAVA_VERSION)"

# macOS specific dependencies
if [[ "$PLATFORM" == "mac" ]]; then
    if ! command -v create-dmg &>/dev/null; then
        if ! command -v brew &>/dev/null; then
            echo "❌ 'create-dmg' not found. Install Homebrew first. Exiting..."
            exit 1
        fi
        read -p "⚠️  'create-dmg' is not installed. Install now? (y/n) " -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            echo "❌ 'create-dmg' is required. Exiting..."
            exit 1
        fi
        echo "📦 Installing 'create-dmg'..."
        BREW_OUTPUT=$(brew install create-dmg 2>&1)
        if [ $? -ne 0 ]; then
            echo "❌ Failed to install 'create-dmg'. Details:"
            echo "$BREW_OUTPUT"
            exit 1
        fi
    fi
fi

# ─── Cleanup ───────────────────────────────────────────────────────────────────

cleanup() {
    log_info "Cleaning up temporary files..."
    [[ -d "$TEMP_DIR" ]] && rm -rf "$TEMP_DIR"
    [[ -d "$PKG_DIR" ]] && rm -rf "$PKG_DIR"
    [[ -d "$TMP_DMG_DIR" ]] && rm -rf "$TMP_DMG_DIR"
}
trap cleanup EXIT

# ─── JRE ───────────────────────────────────────────────────────────────────────

change_dirs "$PROJECT_ROOT/app/$TARGET_MODULE/build"

log_info "Building diet JRE..."
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

JRE_DIR="${TEMP_DIR}/jre"
run_cmd jlink --output "$JRE_DIR" --add-modules "$JRE_MODULES" --strip-debug --no-man-pages --no-header-files --compress=2
if [[ ! -d "$JRE_DIR" ]]; then
    log_error "JLink failed to create JRE."
fi
log_success "Diet JRE built successfully"

# ─── JAR task ──────────────────────────────────────────────────────────────────

change_dirs "$PROJECT_ROOT/app"
log_info "Building Gradle JAR..."
check_file_exists "./gradlew"

GRADLE_OUTPUT=$(./gradlew :$TARGET_MODULE:$JAR_TASK --rerun-tasks 2>&1) || log_error "Gradle build failed."
JAR_FILE=$(echo "$GRADLE_OUTPUT" | awk -F': ' "/${JAR_TASK_OUTPUT_PREFIX}/ {print \$2}")
check_file_exists "$JAR_FILE"

log_success "Shadow JAR task finished"

# ─── JPackage setup ────────────────────────────────────────────────────────────

log_info "Packaging application..."

PKG_DIR="$PROJECT_ROOT/app/build/$OUTPUT_DIR"
rm -rf "$PKG_DIR"
mkdir -p "$PKG_DIR"

COMMON_JPACKAGE_ARGS=(
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --input "$(dirname "$JAR_FILE")"
    --main-jar "$(basename "$JAR_FILE")"
    --runtime-image "$JRE_DIR"
)

# ─── JPackage Mac ──────────────────────────────────────────────────────────────

if [[ "$PLATFORM" == "mac" ]]; then
    run_cmd jpackage "${COMMON_JPACKAGE_ARGS[@]}" --icon "$PROJECT_ROOT/$MAC_ICON" --type dmg --dest "$TEMP_DIR"
    VANILLA_DMG_PATH="$TEMP_DIR/$APP_NAME-$APP_VERSION.dmg"
    check_file_exists "$VANILLA_DMG_PATH"

    log_info "Creating DMG installer..."

    TMP_DMG_DIR="tmp_dmg"
    rm -rf "$TMP_DMG_DIR"
    mkdir -p "$TMP_DMG_DIR"

    # Validate hdiutil operations
    run_cmd hdiutil attach "$VANILLA_DMG_PATH" -mountpoint /Volumes/"$APP_NAME"
    run_cmd cp -r /Volumes/"$APP_NAME"/"$APP_NAME".app "$TMP_DMG_DIR"
    run_cmd hdiutil detach /Volumes/"$APP_NAME"
    rm -f "$VANILLA_DMG_PATH"

    CREATED_DMG_ARGS=(
        --volname "$APP_NAME Installer"
        --background "$PROJECT_ROOT/$BACKGROUND_IMG"
        --window-pos 200 120
        --window-size 600 420
        --icon-size 100
        --icon "$APP_NAME.app" 180 210
        --app-drop-link 430 210
        --format UDBZ
        "$PKG_DIR/$APP_NAME-$APP_VERSION.dmg"
        "$TMP_DMG_DIR"
    )

    run_cmd create-dmg "${CREATED_DMG_ARGS[@]}"
    log_success "Final DMG created."

elif [[ "$PLATFORM" == "windows" ]]; then
    run_cmd jpackage --type exe "${COMMON_JPACKAGE_ARGS[@]}" --win-menu --win-shortcut --icon "$WIN_ICON" --dest "$PKG_DIR"
    check_file_exists "$PKG_DIR/$APP_NAME-$APP_VERSION.exe"
    log_success "EXE installer created in $PKG_DIR"

elif [[ "$PLATFORM" == "linux" ]]; then
    DISTRO=$(lsb_release -is 2>/dev/null || echo "Unknown")
    log_info "Detected Linux distribution: $DISTRO"

    case "$DISTRO" in
    Ubuntu | Debian)
        log_info "Consider using 'dpkg' to create a .deb package"
        ;;
    Fedora | RedHat)
        log_info "Consider using 'rpm' to create an .rpm package"
        ;;
    *)
        log_error "Unsupported Linux distribution!"
        ;;
    esac
else
    log_error "Packiging for $OS not yet supported."
fi

log_success "Packaging complete!"
exit 0
