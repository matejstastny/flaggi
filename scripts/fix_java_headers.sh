#!/bin/bash

# ─────────────────────────────────────────────────────────────────────────────
# Script Name: fix_java_headers.sh
# Location:    /scripts
#
# Description:
# This script recursively scans all Java files in the project root directory.
# For each .java file found, it checks and updates the following:
#
# 1. If the second line contains the text specified in `OLD_USERNAME`,
#    it replaces it with the text in `NEW_USERNAME`.
#
# 2. If the fourth line starts with the text specified in `GIT_PREFIX`,
#    it replaces the entire line with the text in `NEW_GIT_LINK`.
#
# Changes are made in-place and logged to the console.
#
# ─────────────────────────────────────────────────────────────────────────────

# Variables ───────────────────────────────────────────────────────────────────

OLD_USERNAME="aka matysta"
NEW_USERNAME="aka my-daarlin"
NEW_GIT_LINK=" * GitHub link: https://github.com/my-daarlin/flaggi"
# Shouldn't be changed
GIT_PREFIX=" * Git"
ROOT_DIR="$(dirname "$(dirname "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)")")"

# Function to print help ──────────────────────────────────────────────────────

print_help() {
    echo "Usage: fix_java_headers.sh"
    echo
    echo "This script recursively scans all Java files in the hardcoded ROOT_DIR."
    echo
    echo "For each .java file found, the script performs the following updates:"
    echo "  1. Replaces the second line containing '$OLD_USERNAME' with '$NEW_USERNAME'."
    echo "  2. Replaces the fourth line starting with '$GIT_PREFIX' with '$NEW_GIT_LINK'."
}

# Check for -h flag ───────────────────────────────────────────────────────────

if [[ "$1" == "-h" ]]; then
    print_help
    exit 0
fi

# Script ──────────────────────────────────────────────────────────────────────

find "$ROOT_DIR" -type f -name "*.java" | while IFS= read -r file; do
    line2=$(sed -n '2p' "$file")
    line4=$(sed -n '4p' "$file")

    changed=false

    if echo "$line2" | grep -q "$OLD_USERNAME"; then
        sed -i '' "2s/$OLD_USERNAME/$NEW_USERNAME/" "$file"
        changed=true
    fi

    if echo "$line4" | grep -q "^ \* $GIT_PREFIX"; then
        sed -i '' '4s|.*| * GitHub link: $NEW_GIT_LINK|' "$file"
        changed=true
    fi

    if [ "$changed" = true ]; then
        echo "Updated: $file"
    fi
done
