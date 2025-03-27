#!/bin/bash

# Set default TERM value if not set
export TERM=${TERM:-xterm}

#############
# CONSTANTS #
#############

# Directories
ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
SCRIPTS_DIR="$ROOT_DIR/scripts"
ICONS_DIR="$ROOT_DIR/public/icons"
DIET_JRE_DIR="$ROOT_DIR/diet-jre"

APP_DIRS_CLIENT="$ROOT_DIR/client"
APP_DIRS_EDITOR="$ROOT_DIR/editor"

JAR_PATHS_CLIENT="${APP_DIRS_CLIENT}/app/build/libs"
JAR_PATHS_EDITOR="${APP_DIRS_EDITOR}/app/build/libs"

JAR_NAMES_CLIENT="Flaggi.jar"
JAR_NAMES_EDITOR="flaggi-map-editor.jar"

# Resources
ABOUT_URL="https://github.com/kireiiiiiiii/flaggi"
APP_NAME="Flaggi"
SHADOWJAR_TASK="shadowjar"

# Default options
use_diet_jre=true
output_type=""
output_dir=""
icon_path=""
jpackage_exec="jpackage"
jlink_exec="jlink"
mode=""

###########
# METHODS #
###########

# Print help message
usage() {
  echo "Usage: $0 <client|editor> [OPTIONS]"
  echo "Options:"
  echo " -h, --help            Display this help message"
  echo " -n, --nodiet          Package without using the diet JRE"
  echo " --jpackage <path>     Specifies the path to the jpackage executable"
  echo " --jlink <path>        Specifies the path to the jlink executable"
  exit 0
}

# Handle script options
handle_options() {
  while [ $# -gt 0 ]; do
    case $1 in
    client | editor)
      mode="$1"
      ;;
    -h | --help)
      usage
      ;;
    -n | --nodiet)
      use_diet_jre=false
      ;;
    --jpackage)
      shift
      if [[ -z "$1" || "$1" == -* ]]; then
        echo "Error: --jpackage requires a valid path to the jpackage executable." >&2
        usage
        exit 1
      fi
      jpackage_exec="$1"
      ;;
    --jlink)
      shift
      if [[ -z "$1" || "$1" == -* ]]; then
        echo "Error: --jlink requires a valid path to the jlink executable." >&2
        usage
        exit 1
      fi
      jlink_exec="$1"
      ;;
    *)
      echo "Invalid option: $1" >&2
      usage
      exit 1
      ;;
    esac
    shift
  done

  if [ -z "$mode" ]; then
    echo "Error: No mode specified. Use 'client' or 'editor'."
    usage
  fi
}

# Check Java version
check_java_version() {
  local java_version
  java_version=$(java -version 2>&1 | awk -F[\"_] 'NR==1 {print $2}')
  if [[ "$java_version" != "1.8.0" ]]; then
    echo "Error: Java 8 is required to run this script. Current version: $java_version"
    exit 1
  fi
}

# Build the minimal JRE
build_minimal_jre() {
  cd "$ROOT_DIR"

  # Delete existing JRE if it exists
  if [ -d "$DIET_JRE_DIR" ]; then
    rm -rf "$DIET_JRE_DIR"
  fi

  # Use jlink to create the JRE
  echo "Creating minimal JRE..."

  "$jlink_exec" \
    --add-modules java.base,java.desktop \
    --output "$DIET_JRE_DIR" \
    --no-header-files

  echo "JRE created at $DIET_JRE_DIR"
}

###############
# MAIN SCRIPT #
###############

set -e
check_java_version

# Handle options passed to the script
handle_options "$@"

# Determine output format and icon based on OS
case "$OSTYPE" in
darwin*)
  output_type="dmg"
  output_dir="$ROOT_DIR/build/mac"
  icon_path="$ICONS_DIR/mac.icns"
  ;;
msys* | cygwin*)
  output_type="exe"
  output_dir="$ROOT_DIR/build/win"
  icon_path="$ICONS_DIR/win.ico"
  ;;
*)
  echo "Unsupported OS: $OSTYPE"
  exit 1
  ;;
esac

# Set paths based on mode
if [ "$mode" = "client" ]; then
  app_dir="$APP_DIRS_CLIENT"
  jar_path="$JAR_PATHS_CLIENT"
  jar_name="$JAR_NAMES_CLIENT"
elif [ "$mode" = "editor" ]; then
  app_dir="$APP_DIRS_EDITOR"
  jar_path="$JAR_PATHS_EDITOR"
  jar_name="$JAR_NAMES_EDITOR"
fi

# Build the JAR
echo "Building the $mode JAR..."
cd "$app_dir"
./gradlew "app:$SHADOWJAR_TASK"

# Ensure the output directory exists
echo "Ensuring build directory exists at $output_dir..."
mkdir -p "$output_dir"

# Create the output package
if [ -f "$jar_path/$jar_name" ]; then
  if [ "$use_diet_jre" = true ]; then
    # Create the minimal JRE if diet mode is enabled
    echo "Creating minimal JRE for diet packaging..."
    cd "$SCRIPTS_DIR"
    build_minimal_jre

    # Package the app with a diet JRE
    echo "Creating $output_type with diet JRE..."
    "$jpackage_exec" \
      --input "$jar_path" \
      --main-jar "$jar_name" \
      --name "$APP_NAME" \
      --type "$output_type" \
      --dest "$output_dir" \
      --icon "$icon_path" \
      --app-version 1.0 \
      --about-url "$ABOUT_URL" \
      --runtime-image "$DIET_JRE_DIR"
  else
    # Package the app without a diet JRE
    echo "Creating $output_type without diet JRE..."
    "$jpackage_exec" \
      --input "$jar_path" \
      --main-jar "$jar_name" \
      --name "$APP_NAME" \
      --type "$output_type" \
      --dest "$output_dir" \
      --icon "$icon_path" \
      --about-url "$ABOUT_URL" \
      --app-version 1.0
  fi

  # Handle the created file based on the OS
  case "$output_type" in
  dmg)
    echo "DMG created at $output_dir/$APP_NAME.dmg"
    ;;
  exe)
    echo "EXE created at $output_dir/$APP_NAME.exe"
    ;;
  *)
    echo "Error: Unsupported output format ($output_type)"
    exit 1
    ;;
  esac
else
  echo "Error: JAR file not found at $jar_path/$jar_name"
  exit 1
fi
