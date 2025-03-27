#!/bin/bash

# Set default TERM value if not set
export TERM=${TERM:-xterm}

###############
#  CONSTANTS  #
###############

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
DIET_JRE="$PROJECT_ROOT/diet-jre"

APP_DIRS_CLIENT="$PROJECT_ROOT/client"
APP_DIRS_SERVER="$PROJECT_ROOT/server"
APP_DIRS_EDITOR="$PROJECT_ROOT/editor"

JAR_FILES_CLIENT="${APP_DIRS_CLIENT}/app/build/libs/Flaggi.jar"
JAR_FILES_SERVER="${APP_DIRS_SERVER}/app/build/libs/Flaggi-server.jar"
JAR_FILES_EDITOR="${APP_DIRS_EDITOR}/app/build/libs/flaggi-map-editor.jar"

###############
#  VARIABLES  #
###############

use_diet_jre=true
selected_mode=""
host_ip=""
should_clear_screen=false

###############
#   METHODS   #
###############

# Display help message
usage() {
  echo "Usage: $0 <client|server|docker|editor> [OPTIONS]"
  echo "Options:"
  echo "  -h, --help      Display this help message."
  echo "  -n, --nodiet    Don't use diet JRE, but a normal one."
  echo "  -c, --clear     Clear the screen before running the script."
  exit 0
}

# Handle command-line options and arguments
handle_options() {
  local mode_count=0

  while [ $# -gt 0 ]; do
    case $1 in
    client | server | docker | editor)
      mode_count=$((mode_count + 1))
      selected_mode="$1"
      ;;
    -h | --help)
      usage
      ;;
    -n | --nodiet)
      use_diet_jre=false
      ;;
    -c | --clear)
      should_clear_screen=true
      ;;
    *)
      echo "Invalid option: $1" >&2
      usage
      ;;
    esac
    shift
  done

  # Ensure only one mode is specified
  if [ "$mode_count" -gt 1 ]; then
    echo "Error: Cannot specify multiple modes (client, server, docker, editor) at once."
    usage
  fi
}

# Print a divider line
print_divider() {
  width=$(tput cols)
  printf '%*s\n' "$width" '' | tr ' ' '-'
}

# Build the minimal JRE using jlink
build_minimal_jre() {
  cd "$PROJECT_ROOT"

  # Delete existing JRE if it exists
  if [ -d "$DIET_JRE" ]; then
    rm -rf "$DIET_JRE"
  fi

  # Use jlink to create the JRE
  echo "Creating minimal JRE..."

  jlink \
    --add-modules java.base,java.desktop \
    --output "$DIET_JRE" \
    --no-header-files

  echo "JRE created at $DIET_JRE"
}

# Check if the Java version is 1.8.0
check_java_version() {
  local java_version
  java_version=$(java -version 2>&1 | awk -F[\"_] 'NR==1 {print $2}')
  if [[ "$java_version" != "1.8.0" ]]; then
    echo "Error: Java 8 is required to run this script. Current version: $java_version"
    exit 1
  fi
}

# Build and run the specified application
build_and_run() {
  local app_name=$1
  local app_dir
  local jar_file

  case $app_name in
    client)
      app_dir="$APP_DIRS_CLIENT"
      jar_file="$JAR_FILES_CLIENT"
      ;;
    server)
      app_dir="$APP_DIRS_SERVER"
      jar_file="$JAR_FILES_SERVER"
      ;;
    editor)
      app_dir="$APP_DIRS_EDITOR"
      jar_file="$JAR_FILES_EDITOR"
      ;;
  esac

  # Build JAR
  echo "Building the $app_name JAR..."
  cd "$app_dir"
  ./gradlew shadowjar

  # Run the JAR
  if [ -f "$jar_file" ]; then
    if [ "$use_diet_jre" = true ]; then
      echo "Making the diet JRE ..."
      cd "$SCRIPT_DIR"
      build_minimal_jre
      echo "Running the $app_name using diet JRE..."
      cd "$(dirname "$jar_file")"
      print_divider
      echo ""
      "$DIET_JRE/bin/java" -jar "$(basename "$jar_file")"
    else
      echo "Running the $app_name using global JRE..."
      cd "$(dirname "$jar_file")"
      echo ""
      print_divider
      java -jar "$(basename "$jar_file")"
    fi
  else
    echo "Error: JAR file not found at $jar_file"
    exit 1
  fi
}

# Run the Docker container for the server
run_docker() {
  # Get the host's IP address
  host_ip=$(ifconfig | grep 'inet ' | awk '/inet / {print $2}' | grep -Ev '^(127\.|::)')

  # Build server JAR
  echo "Building the server JAR..."
  cd "$APP_DIRS_SERVER"
  ./gradlew shadowjar

  # Build the docker image
  cd "$PROJECT_ROOT"
  echo "Building Docker image..."
  docker build -t flaggi-server .

  # Stop and remove any existing container with the same name
  if [ "$(docker ps -aq -f name=flaggi-server)" ]; then
    docker stop flaggi-server
    docker rm flaggi-server
  fi

  # Run the docker container & expose the ports used by the server
  echo "Running the Docker container..."
  docker run \
    --name flaggi-server \
    -p 54321:54321/tcp \
    -p 54322:54322/udp \
    -e HOST_IP=$host_ip \
    flaggi-server
}

###############
# MAIN SCRIPT #
###############

set -e
check_java_version
handle_options "$@"

if [ -z "$selected_mode" ]; then
  echo "Error: No mode specified. Use 'client', 'server', 'docker', or 'editor'."
  usage
fi

if [ "$should_clear_screen" = true ]; then
  clear
fi

if [ "$selected_mode" = "client" ] || [ "$selected_mode" = "server" ] || [ "$selected_mode" = "editor" ]; then
  build_and_run "$selected_mode"
elif [ "$selected_mode" = "docker" ]; then
  run_docker
fi
