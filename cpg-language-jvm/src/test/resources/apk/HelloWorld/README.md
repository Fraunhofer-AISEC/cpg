# HelloWorld APK Test Resource

This directory contains a minimal APK file used for testing the JVM frontend's error handling improvements (PR #2545).

## Structure

- `MainActivity.java` - Simple Java class mimicking an Android Activity
- `build.sh` - Build script to create the APK
- `app-debug.apk` - The generated APK file (JAR format)

## Building the APK

To rebuild the APK after modifying `MainActivity.java`:

```bash
./build.sh
```

The script will:
1. Clean previous build artifacts
2. Compile `MainActivity.java` using `javac`
3. Package the `.class` file into `app-debug.apk` using `jar`
4. Display the APK contents and size

## What is this APK?

This is a minimal "APK" that is actually just a JAR file containing a single class file. Real Android APKs are more complex (they contain DEX files, resources, manifests, etc.), but for testing the JVM frontend's error handling, this simple structure is sufficient.

The APK is used in `JVMLanguageFrontendTest.testHelloWorldApk()` to verify that:
- The JVM frontend can parse APK/JAR files
- Error handling improvements prevent out-of-memory errors
- Package filtering configuration works correctly

## Requirements

- Java JDK (for `javac` and `jar` commands)

## Notes

- The APK is committed to the repository for easy test execution
- If you modify `MainActivity.java`, run `./build.sh` to regenerate the APK
- The build is deterministic - the same source will produce the same APK

