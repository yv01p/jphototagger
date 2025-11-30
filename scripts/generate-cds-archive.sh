#!/bin/bash

# Generate Class Data Sharing (CDS) archive for JPhotoTagger
# This speeds up application startup by pre-loading classes

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$(dirname "$SCRIPT_DIR")"
JAR_FILE="$APP_HOME/Program/build/libs/Program.jar"
CDS_ARCHIVE="$APP_HOME/lib/jphototagger.jsa"
CLASS_LIST="$APP_HOME/build/jphototagger.classlist"

# Create lib directory if needed
mkdir -p "$APP_HOME/lib"

echo "Step 1: Creating class list..."
java -Xshare:off \
     -XX:DumpLoadedClassList="$CLASS_LIST" \
     -jar "$JAR_FILE" --dry-run 2>/dev/null &
PID=$!

# Wait briefly then terminate (we just need class list)
sleep 5
kill $PID 2>/dev/null

if [ ! -f "$CLASS_LIST" ]; then
    echo "Error: Failed to generate class list"
    exit 1
fi

echo "Step 2: Creating CDS archive..."
java -Xshare:dump \
     -XX:SharedClassListFile="$CLASS_LIST" \
     -XX:SharedArchiveFile="$CDS_ARCHIVE" \
     -cp "$JAR_FILE"

if [ -f "$CDS_ARCHIVE" ]; then
    echo "CDS archive created: $CDS_ARCHIVE"
    echo "Size: $(ls -lh "$CDS_ARCHIVE" | awk '{print $5}')"
else
    echo "Error: Failed to create CDS archive"
    exit 1
fi
