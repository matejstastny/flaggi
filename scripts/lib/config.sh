#!/usr/bin/env bash

# --------------------------------------------------------------------------------------------
# Global configuration for all build/packaging scripts
# --------------------------------------------------------------------------------------------

# Root -----------------------------------------------------------------------

PROJECT_ROOT="$(realpath "$(dirname "$0")/..")"

# Application ----------------------------------------------------------------

APP_NAME="Flaggi"
MAC_ICON="$PROJECT_ROOT/assets/icons/flaggi.icns"
DMG_BACKGROUND="$PROJECT_ROOT/assets/banners/dmg-background.png"

# Java -----------------------------------------------------------------------

JAVA_VERSION="21"
JRE_MODULES="java.base,java.desktop"

# Directories ----------------------------------------------------------------

DIR_BUILD="$PROJECT_ROOT/build"
DIR_DIST="$PROJECT_ROOT/distribution"
DIR_APP="$DIR_BUILD/temp"
DIR_SHADOWJAR="$PROJECT_ROOT/shadowjar"
DIR_DMG_STAGE="$DIR_TEMP/dmg-stage"
DIR_SERVER_TEMP="$PROJECT_ROOT/server-temp"
