#!/bin/bash

# Build script for creating a minimal APK for testing
# This APK is used to test the JVM frontend's error handling improvements

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "Building HelloWorld APK..."

# Clean previous build artifacts
rm -f MainActivity.class
rm -f app-debug.apk
rm -rf classes/

# Compile the Java source
echo "Compiling MainActivity.java..."
javac MainActivity.java

# Verify compilation
if [ ! -f "MainActivity.class" ]; then
    echo "Error: Compilation failed, MainActivity.class not found"
    exit 1
fi

# Create APK (which is just a JAR file)
echo "Creating app-debug.apk..."
jar -cf app-debug.apk MainActivity.class

# Verify APK was created
if [ ! -f "app-debug.apk" ]; then
    echo "Error: APK creation failed"
    exit 1
fi

# Show APK contents
echo "APK created successfully!"
echo "Contents:"
jar -tf app-debug.apk

echo ""
echo "APK size: $(du -h app-debug.apk | cut -f1)"
echo "Build complete!"

