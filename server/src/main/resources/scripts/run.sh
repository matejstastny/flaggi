#!/bin/bash

set -e
set -u

# ───────────────────────────────────────────────────────────────────────────────
# Run Script for "Flaggi" server
# Runs server with the RAM value configured in config.properties
# ───────────────────────────────────────────────────────────────────────────────

usage() {
    echo "Usage: $0 <server-jar> [config-file]"
    echo "Runs the Flaggi server with the specified JAR and optional configuration file."
    echo
    echo "Arguments:"
    echo "  <server-jar>     Path to the server JAR file."
    echo "  [config-file]    Optional path to config.properties (default: ./config.properties)."
    exit 0
}

# Check for help flag
if [[ $# -gt 0 && ("$1" == "-h" || "$1" == "--help") ]]; then
    usage
fi

# Validate arguments
if [[ $# -lt 1 ]]; then
    echo "❌  Missing required argument: <server-jar>"
    usage
fi

JAR_FILE="$1"
CONFIG_FILE="${2:-config.properties}"

if [[ ! -f "$JAR_FILE" ]]; then
    echo "❌  Error: JAR file '$JAR_FILE' not found!"
    exit 1
fi

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "❌  Error: Config file '$CONFIG_FILE' not found. Using defaults"
    exit 1
fi

# ─── Load Configured RAM Size ─────────────────────────────────────────────────

RAM=$(awk -F'=' '/^java.ram=/ {print $2}' "$CONFIG_FILE" 2>/dev/null || echo "")

if [[ -z "$RAM" ]]; then
    echo "⚠️  RAM size not specified in config.properties (java.ram)"
    echo "📦  Using default: 1G"
    RAM="1G"
fi

if [[ ! "$RAM" =~ ^[0-9]+[KMG]?$ ]]; then
    echo "❌  Error: Invalid RAM format '$RAM'. Use a number followed by K, M, or G (e.g., 512M, 2G)."
    exit 1
fi

# ─── Start Server ─────────────────────────────────────────────────────────────

echo "🚀 Starting Flaggi server with $RAM of RAM..."
printf "%$(tput cols)s" '' | tr ' ' -
exec java -Xmx"$RAM" -jar "$JAR_FILE"
