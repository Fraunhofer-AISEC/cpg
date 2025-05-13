# Environment Setup for Developing the Project

This document outlines the steps and tools required to set up a development environment for the Kotlin + Gradle multi-module project, using Cursor as the primary editor.

---

## 1. Required Tools and Installations

### Homebrew (macOS)

Install Homebrew if not already installed:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### Kotlin CLI

Install the Kotlin compiler via Homebrew:

```bash
brew install kotlin
```

Verify installation:

```bash
kotlin -version
```

### Gradle CLI

Install Gradle via Homebrew:

```bash
brew install gradle
```

Verify installation:

```bash
gradle -v
```

---

## 2. Recommended VS Code / Cursor Extensions

| Extension Name                   | Purpose                                  |
| -------------------------------- | ---------------------------------------- |
| `Kotlin` (by JetBrains)          | Kotlin language support and IntelliSense |
| `Kotlin Formatter`               | Formatting support for Kotlin            |
| `Gradle for Java` (by Microsoft) | Support for Gradle tasks and navigation  |

**Do Not Install:**

* `Language Support for Java(TM) by Red Hat` – causes false positive linter errors in mixed Kotlin/Java projects. Disable or uninstall it if already installed.

---

## 3. Project Structure Expectations

Ensure each module follows the standard structure:

```
module-root/
├── build.gradle.kts
├── src/
│   └── main/kotlin/com/your/package/...kt
│   └── test/kotlin/com/your/package/...kt
```

If the module is part of a larger multi-module project, ensure it is included in `settings.gradle.kts` at the root level.

---

## 4. Gradle Build

To build the project:

```bash
./gradlew build
```

This compiles the entire multi-module project and ensures the Kotlin Language Server picks up correct classpaths and types.

---

## 5. Editor Configuration

Optionally, disable Java validation in `.vscode/settings.json` (recommended for consistency across teams):

```json
{
  "java.validate.enable": false,
  "java.errors.incompleteClasspath.severity": "ignore"
}
```

Add this to the root project's `.vscode/` folder if needed.

---

## 6. Kotlin Language Server

The `Kotlin` extension from JetBrains automatically starts the Kotlin Language Server. No manual installation or setup is required.

---

## 7. Summary

* ✅ Kotlin and Gradle should be installed via Homebrew.
* ✅ Use the `Kotlin` and `Gradle for Java` extensions in Cursor.
* ❌ Avoid using the Red Hat Java extension.
* ✅ Ensure project compiles via `./gradlew build`.
* ✅ Use optional `.vscode/settings.json` to suppress Java linting errors.
