#!/bin/bash

# JPhotoTagger Launch Script
# Optimized JVM settings for Java 21

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$(dirname "$SCRIPT_DIR")"

# JVM Options for optimal performance
JVM_OPTS=(
    # ZGC for low-latency garbage collection
    -XX:+UseZGC
    # String deduplication reduces memory usage
    -XX:+UseStringDeduplication
    # Memory settings
    -Xmx1g
    -Xms256m
    # Class Data Sharing (if archive exists)
    -XX:SharedArchiveFile="$APP_HOME/lib/jphototagger.jsa"
)

# Check if CDS archive exists
if [ ! -f "$APP_HOME/lib/jphototagger.jsa" ]; then
    # Remove CDS option if archive doesn't exist
    JVM_OPTS=("${JVM_OPTS[@]/-XX:SharedArchiveFile=*/}")
fi

exec java "${JVM_OPTS[@]}" -jar "$APP_HOME/lib/jphototagger.jar" "$@"
