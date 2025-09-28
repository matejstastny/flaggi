#!/usr/bin/env bash
set -euo pipefail

AUTHOR="Matej Stastny"
LICENSE="MIT"
LINK="https://github.com/matejstastny/flaggi"

find . -type f -name "*.java" | while read -r file; do
    if grep -q "^/\*" "$file"; then
        # Extract the date line
        date_line=$(sed -n 's/.*Date[^:]*:\s*//p' "$file" | head -n1)

        # Base date (before any parentheses)
        base_date=$(echo "$date_line" | sed -E 's/\(.*//; s/ .*//')
        base_date=$(date -j -f "%m/%d/%Y" "$base_date" "+%Y-%m-%d" 2>/dev/null || echo "$base_date")

        # v2 (optional)
        v2_date=$(echo "$date_line" | grep -o 'v[0-9]\s*-\s*[0-9/]*' | sed -E 's/v[0-9]\s*-\s*//')
        if [[ -n "$v2_date" ]]; then
            v2_date=$(date -j -f "%m/%d/%Y" "$v2_date" "+%m-%d-%Y" 2>/dev/null || echo "$v2_date")
            v2_part=" (2.0: $v2_date)"
        else
            v2_part=""
        fi

        # Filename
        fname=$(basename "$file")

        # New header
        new_header=$(
            cat <<EOF
// ------------------------------------------------------------------------------
// $fname - Main application class
// ------------------------------------------------------------------------------
// Author: $AUTHOR
// Date: $base_date$v2_part
// License: $LICENSE
// Link: $LINK
// ------------------------------------------------------------------------------
EOF
        )

        # Strip old /* ... */ header and insert new one
        tmpfile=$(mktemp)
        awk '
      BEGIN {skip=0}
      NR==1 && /^\/\*/ {skip=1; next}
      skip && /\*\// {skip=0; next}
      !skip {print}
    ' "$file" >"$tmpfile"

        {
            echo "$new_header"
            cat "$tmpfile"
        } >"$file"
        rm "$tmpfile"

        echo "Updated: $file"
    fi
done
