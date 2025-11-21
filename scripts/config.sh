#!/usr/bin/env bash

# Config -------------------------------------------------------------------------------------

PROJECT_ROOT="$(realpath "$(dirname "$0")/..")" # Project root path
JAVA_VER="21"                                   # Java version needed to build the project
SERVER_DIR="flaggi-server-temp"                 # Name of the test server directory
