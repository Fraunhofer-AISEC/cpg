
# Environment Setup for Kotlin + Gradle Development in Cursor

This guide outlines the recommended environment setup to develop Kotlin and Gradle projects using Cursor (a VSCode-based editor).

---

## 🧰 Tooling Setup (macOS)

### 1. Install Kotlin & Gradle (via Homebrew)
```bash
brew install kotlin gradle
```

This provides access to `kotlin` and `gradle` commands in the terminal.

### 2. Enable Project Build
You should be able to build the project via:
```bash
./gradlew build
```

This ensures Gradle is bootstrapped using the wrapper bundled with the project.

---

## ⚙️ Recommended Cursor Extensions
Install the **"Kotlin on VSCode" extension pack by Seth Jones** which includes:

- ✅ `Kotlin Formatter` by **cstef** (used for `.kt` and `.kts` formatting)
- ✅ `Gradle Language Support` by **Naco Siren** (for `build.gradle.kts` syntax and completions)
- ✅ `Kotlin Language` by **mathiasfrohlich`** (basic syntax support)

> ❌ Disabled: `Kotlin` by **fwcd** — caused excessive memory usage due to the Kotlin Language Server (KLS).
> ❌ Disabled: `ktfmt` — unnecessary heavy formatter running via Java.
> ❌ Removed: `Code Runner` — not used and can spawn redundant JVM instances.

---

## 🧠 Memory Optimization
To prevent Java-based extensions (like the Kotlin Language Server) from slowing down your system:

### 1. Disable Language Server in `.vscode/settings.json`
```json
{
  "java.validate.enable": false,
  "java.errors.incompleteClasspath.severity": "ignore"
}
```

> ✅ It's safe to completely remove `kotlin.languageServer.jvmArgs` if you're no longer using the fwcd Kotlin extension.
> ✅ Also remove `ktfmt.path-to-jar` if not using ktfmt.

### 2. Monitor Activity Monitor
Check for multiple `java` processes in Activity Monitor. If they persist after closing Cursor:
- Kill them manually
- Or reboot to clean up stuck background servers

---

## 🧪 Troubleshooting
- To detect actual Kotlin compile-time errors: use `./gradlew build`
- The linter in Cursor may **not** show all semantic/compile errors unless a full compile is run.

---

## 📁 Project Structure Notes
- Your subproject should contain its own `build.gradle.kts` using shared Gradle conventions.
- Place this `environment.md` file under `docs/` folder for future reference.

---

Feel free to update this doc as your dev workflow evolves.
