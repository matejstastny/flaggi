#!/usr/bin/env bash

# --------------------------------------------------------------------------------------------
# shared.sh - Library of methods used in other flaggi scripts
# --------------------------------------------------------------------------------------------
# Author: Matej Stastny
# Date: 2025-11-22 (YYYY-MM-DD)
# License: MIT
# Link: https://github.com/matejstastny/flaggi
# --------------------------------------------------------------------------------------------

set -e
source "$(dirname "$0")/lib/config.sh"
source "$(dirname "$0")/lib/logging.sh"

# Checks if the current java version matches the required one.
# @param Target Java version
# @return 1 if check fails
check_java_ver() {
    local java_cmd
    java_cmd=$(command -v java || true)
    if [[ -z "$java_cmd" ]]; then
        log error "Java not found."
        return 1
    fi

    local version
    version=$("$java_cmd" -version 2>&1 | awk -F'"' '/version/ {print $2}' | awk -F'.' '{print ($1=="1"?$2:$1)}')

    if ! [[ "$version" =~ ^[0-9]+$ ]]; then
        log error "Could not parse Java version: $version"
        return 1
    fi

    if [[ "$version" -ne "$1" ]]; then
        log error "Java $1 required, found $version at $java_cmd."
        return 1
    fi

    log info "Java $version at $java_cmd."
    return 0
}

# Prints the gradle project version using `gradle properties`.
get_project_ver() {
    local gradle_cmd="$PROJECT_ROOT/gradlew"
    [[ -x "$gradle_cmd" ]] || gradle_cmd="gradle"

    "$gradle_cmd" properties --quiet 2>/dev/null |
        awk -F': ' '/^version:/ {print $2}'
}

# Runs the `gradle shadowJar` task.
# @return 1 if build fails
run_shadowjar() {
    log info "Building shadowJar..."

    local gradle_cmd="$PROJECT_ROOT/gradlew"
    [[ -x "$gradle_cmd" ]] || gradle_cmd="gradle"

    (cd "$PROJECT_ROOT" && "$gradle_cmd" shadowJar --quiet) || {
        log error "shadowJar build failed"
        return 1
    }

    log done "shadowJar built"
    return 0
}

# Returns the path to the built shadowJar for the current TARGET_MODULE.
# @return 1 if no JAR found
get_shadowjar_path() {
    local jar_file
    jar_file=$(find "$DIR_SHADOWJAR" -maxdepth 1 -type f -name "flaggi-${TARGET_MODULE}*.jar" | head -n 1) || true
    if [[ -z "$jar_file" ]]; then
        log error "No JAR found matching: flaggi-${TARGET_MODULE}*.jar"
        return 1
    fi
    echo "$jar_file"
}

# Runs a command silently, logging output only on failure.
run_quiet() {
    local cmd=("$@")
    set +e
    local output
    output=$("${cmd[@]}" 2>&1)
    local status=$?
    set -e

    if [[ $status -ne 0 ]]; then
        log error "Command failed: ${cmd[*]}"
        [[ -n "$output" ]] && log error "$output"
        return $status
    fi
}

# Scans required Java modules for a given JAR using jdeps.
# @param Path to JAR file
# @return 1 if scan fails
scan_required_modules() {
    local jar="$1"
    set +e
    local mods
    mods=$(jdeps --multi-release "$JAVA_VERSION" --print-module-deps "$jar" 2>/dev/null)
    set -e

    if [[ -z "$mods" ]]; then
        log error "Module scan failed for $jar."
        return 1
    fi
    echo "$mods"
}
