#!/bin/bash

# Generate Class Data Sharing (CDS) archive for JPhotoTagger
# This speeds up application startup by pre-loading classes

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$(dirname "$SCRIPT_DIR")"
JAR_FILE=$(ls "$APP_HOME"/Program/build/libs/Program*.jar 2>/dev/null | head -1)
CDS_ARCHIVE="$APP_HOME/lib/jphototagger.jsa"

# Validate JAR exists
if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
    echo "Error: Program JAR not found. Run './gradlew :Program:jar' first"
    exit 1
fi

# Create lib directory if needed
mkdir -p "$APP_HOME/lib"

echo "Using JAR: $JAR_FILE"
echo "Generating CDS archive using -XX:ArchiveClassesAtExit..."

# Use ArchiveClassesAtExit for simpler approach
# App will start and create archive on JVM exit
timeout 15s java -XX:ArchiveClassesAtExit="$CDS_ARCHIVE" \
     -jar "$JAR_FILE" 2>/dev/null &
APP_PID=$!

# Wait for app to initialize
sleep 10

# Gracefully terminate (allows archive generation on exit)
kill -TERM $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null

if [ -f "$CDS_ARCHIVE" ]; then
    echo "CDS archive created: $CDS_ARCHIVE"
    echo "Size: $(ls -lh "$CDS_ARCHIVE" | awk '{print $5}')"
    exit 0
else
    echo "Error: Failed to create CDS archive"
    exit 1
fi
