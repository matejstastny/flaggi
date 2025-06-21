#!/bin/bash

# ─────────────────────────────────────────────────────────────────────────────
# Script Name: fix_java_headers.sh
# Location:    /scripts
#
# Description:
# This script recursively scans all Java files in the project root directory.
# For each .java file found, it checks and updates the following:
#
# 1. If the second line contains the text "aka Kirei", it replaces it with:
#      "aka matysta" (to update my new github username)
#
# 2. If the fourth line starts with " * Git", it replaces the entire line with:
#      " * GitHub link: https://github.com/matysta/flaggi"
#
# Changes are made in-place and logged to the console.
#
# ─────────────────────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

find "$ROOT_DIR" -type f -name "*.java" | while read -r file; do
    mapfile -t lines < <(head -n 5 "$file")
    changed=false

    if [[ "${lines[1]}" == *"aka Kirei"* ]]; then
        sed -i '' '2s/aka Kirei/aka matysta/' "$file"
        changed=true
    fi

    if [[ "${lines[3]}" == " * Git"* ]]; then
        sed -i '' '4s|.*| * GitHub link: https://github.com/matysta/flaggi|' "$file"
        changed=true
    fi

    if [ "$changed" = true ]; then
        echo "Updated: $file"
    fi
done
