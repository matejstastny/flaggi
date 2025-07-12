#!/bin/bash

# ─────────────────────────────────────────────────────────────────────────────
# Script Name: fix_java_headers.sh
# Location:    /scripts
#
# Description:
# This script recursively scans all Java files in the project root directory.
# For each .java file found, it checks if the first line starts with a specific
# string (`HEADER_START`) and if the target line (specified by `TARGET_LINE`)
# starts with another specific string (`HEADER_END`). If the block comment
# spans only from the first line to the target line, it replaces the entire
# block with a new header (`NEW_HEADER`).
#
# Changes are made in-place and logged to the console.
#
# ─────────────────────────────────────────────────────────────────────────────

# Variables ───────────────────────────────────────────────────────────────────

HEADER_START="/*"
HEADER_END=" */"
TARGET_LINE=5
DEFAULT_DATE="0/0/0000"

make_new_header() {
    local date="$1"
    cat <<EOF
/*
 * Author: Matěj Šťastný aka my-daarlin
 * Date created: $date
 * GitHub link: https://github.com/my-daarlin/flaggi
 */
EOF
}
ROOT_DIR="$(dirname "$(dirname "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)")")"

# Function to print help ──────────────────────────────────────────────────────

print_help() {
    echo "Usage: fix_java_headers.sh"
    echo
    echo "This script recursively scans all Java files in the hardcoded ROOT_DIR."
    echo
    echo "For each .java file found, the script performs the following updates:"
    echo "  1. Checks if the first line starts with '$HEADER_START'."
    echo "  2. Checks if line $TARGET_LINE starts with '$HEADER_END'."
    echo "  3. If the block comment spans only from the first line to line $TARGET_LINE,"
    echo "     it replaces the block with the new header."
}

# Check for -h flag ───────────────────────────────────────────────────────────

if [[ "$1" == "-h" ]]; then
    print_help
    exit 0
fi

# Script ──────────────────────────────────────────────────────────────────────

echo "Starting script execution..."
echo "Scanning directory: $ROOT_DIR"
echo "Looking for Java files..."

find "$ROOT_DIR" -type f -name "*.java" | while IFS= read -r file; do
    echo "Processing file: $file"
    first_line=$(sed -n '1p' "$file")
    target_line=$(sed -n "${TARGET_LINE}p" "$file")

    echo "Checking first line for HEADER_START..."
    if echo "$first_line" | grep -q "^$HEADER_START"; then
        echo "First line starts with HEADER_START."
        echo "Checking target line ($TARGET_LINE) for HEADER_END..."
        if echo "$target_line" | grep -q "^\s*\*/"; then
            echo "Target line starts with HEADER_END."
            echo "Extracting date from existing header..."
            date_line=$(sed -n '/Date created:/s/.*Date created: //p' "$file" | head -n1)
            if [[ -z "$date_line" ]]; then
                date_line="$DEFAULT_DATE"
            fi
            new_header=$(make_new_header "$date_line")
            echo "$new_header" >"$file.tmp"
            tail -n +"$((TARGET_LINE + 1))" "$file" >>"$file.tmp"
            mv "$file.tmp" "$file"
            echo "Updated: $file"
        else
            echo "Target line does not start with HEADER_END. No changes made."
        fi
    else
        echo "First line does not start with HEADER_START. No changes made."
    fi
done

echo "Script execution completed."
