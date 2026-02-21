#!/usr/bin/env bash

# --------------------------------------------------------------------------------------------
# shared.sh - library of methods used in other flaggi scripts
# --------------------------------------------------------------------------------------------
# Author: Matej Stastny
# Date: 2025-11-22 (YYYY-MM-DD)
# License: MIT
# Link: https://github.com/matejstastny/flaggi
# --------------------------------------------------------------------------------------------

set -e
source "$(dirname "$0")/lib/config.sh"
source "$(dirname "$0")/lib/logging.sh"

# checks if the current java used matches
# @param Target Java version
# @return 1 if fail
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
        log error "Could not parse java version: $version"
        return 1
    fi

    if [[ "$version" -ne "$1" ]]; then
        log error "Java $1 required; found $version at $java_cmd"
        return 1
    fi

    log info "Java version: $version at $java_cmd"
    return 0
}

# prints gradle project version using `gradle properties`
get_project_ver() {
    local gradle_cmd
    gradle_cmd="$PROJECT_ROOT/gradlew"
    if [[ ! -x "$gradle_cmd" ]]; then
        gradle_cmd="gradle"
    fi

    local project_version
    project_version=$(
        "$gradle_cmd" properties --quiet 2>/dev/null |
            awk -F': ' '/^version:/ {print $2}'
    )

    echo "$project_version"
}

# runs the `gradle shadowjar` tasks
run_shadowjar() {
    log info "Running shadowjar..."

    # prefer project wrapper if available, otherwise use system gradle
    local gradle_cmd="$PROJECT_ROOT/gradlew"
    if [[ ! -x "$gradle_cmd" ]]; then
        gradle_cmd="gradle"
    fi

    (cd "$PROJECT_ROOT" && "$gradle_cmd" shadowJar --quiet) || {
        log error "shadowJar failed."
        return 1
    }

    log success "shadowjar finished"
    return 0
}

# gets shadowjar archive path
# @param <client|server|editor> which jar
# @ return 1 if fails
get_shadowjar_path() {
    local jar_file
    jar_file=$(find "$DIR_SHADOWJAR" -maxdepth 1 -type f -name "flaggi-${TARGET_MODULE}*.jar" | head -n 1) || true
    if [[ -z "$jar_file" ]]; then
        log error "No JAR found for pattern: flaggi-${TARGET_MODULE}*.jar"
        return 1
    fi
    echo "$jar_file"
}

# logs only if debug enabled
# usage: log_debug "message"
log_debug() {
    if [[ "${DEBUG:-0}" -eq 1 ]]; then
        log info "$1"
    fi
}

run_quiet() {
    local cmd=("$@")

    log_debug "Running command: ${cmd[*]}"

    # Disable set -e inside this function
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

scan_required_modules() {
    local jar="$1"
    set +e
    local mods
    mods=$(jdeps --multi-release "$JAVA_VERSION" --print-module-deps "$jar" 2>/dev/null)
    set -e
    if [[ -z "$mods" ]]; then
        log error "Module scan failed for $jar"
        return 1
    fi
    echo "$mods"
}
