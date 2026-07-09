---
title: "Project API"
linkTitle: "Project API"
weight: 2
date: 2026-01-01
description: >
    High-level API for configuring and running CPG analyses
---

The **Project API** is the recommended, high-level entry point for building a CPG from source code.
It handles language registration, pass configuration, and component detection automatically, so you
only need to override the parts that differ from the defaults.

## Quick start

The simplest possible analysis — pass a directory and get a result:

```kotlin
val result = project(Path("/path/to/repo")) { }.analyze()
```

This **auto-mode** uses every language frontend available on the classpath, the full default pass
pipeline, and runs all registered auto-detectors to discover the project structure (Go modules,
C/C++ compilation databases, etc.).

For a single file you don't need a project at all:

```kotlin
val result = Project.from(Path("main.cpp")).analyze()
```

## Auto-mode vs. explicit override

All three main configuration blocks — `languages {}`, `passes {}`, and `components {}` — follow the
same pattern:

| Block | Not called | Called without `default()` | Called with `default()` |
|---|---|---|---|
| `languages {}` | All default languages on the classpath | Only the listed languages | Default languages + listed languages |
| `passes {}` | Default pass pipeline | Only the listed passes | Default passes + listed passes |
| `components {}` | Language detectors run, auto-detect | Explicit components only, no detection | Auto-detect + explicit components |

## Languages

```kotlin
// Auto-mode: every language frontend available on the classpath is used.
project(path) { }

// Explicit: only Go is registered.
project(path) {
    languages { use<GoLanguage>() }
}

// Combination: all defaults plus an extra language (e.g., an in-house frontend).
project(path) {
    languages { default(); use<MyCustomLanguage>() }
}
```

## Passes

```kotlin
// Auto-mode: the full default pass pipeline runs.
project(path) { }

// No default passes; only the explicitly listed ones run.
// Useful for raw-AST inspection or very fast single-pass analyses.
project(path) {
    passes { use<SymbolResolver>() }
}

// Default passes plus a custom one.
project(path) {
    passes { default(); use<MyCustomPass>() }
}
```

## Components

A *component* maps to a [`Component`](../CPG/specs/graph.md) node in the graph. By default the
project auto-detects components through language-specific detectors (e.g., `go.mod` files for Go,
`compile_commands.json` for C/C++).

```kotlin
// Auto-mode: detectors discover the components.
project(path) { }

// Explicit: a single "backend" component — no detection runs.
project(path) {
    components {
        component("backend", root = path.resolve("services/backend"))
    }
}

// Auto-detect AND add an extra component (e.g., a generated stub directory).
project(path) {
    components {
        default()
        component("stubs", root = path.resolve("generated/stubs"))
    }
}

// Disable auto-detection entirely (empty block).
project(path) {
    components { }
}
```

### Standalone detectors

[`DirectoryComponentDetector`][1] creates one component per direct subdirectory of a given folder.
It is useful for convention-based monorepos:

```kotlin
project(path) {
    components {
        detector(DirectoryComponentDetector("services"))
    }
}
```

Adding a `detector()` inside the block automatically enables auto-detection (equivalent to calling
`default()` first).

## Environment

Specify the target OS and architecture when analysing cross-compiled code:

```kotlin
project(path) {
    environment {
        os = OperatingSystem.LINUX
        architecture = Architecture.ARM64
    }
}
```

The environment is forwarded to [project detectors][2] so they can derive the correct build
constraints (e.g., `GOOS`/`GOARCH` symbols for Go).

## Exclusions

```kotlin
project(path) {
    exclude("vendor", "testdata", "node_modules")
    exclude(Regex(".*_test\\.go"))
}
```

## Low-level escape hatch

For options not yet exposed by the Project API, use `translation {}` to modify the underlying
[`TranslationConfiguration`](library.md) directly. This modifier runs after all project-level
settings, so it takes precedence:

```kotlin
project(path) {
    translation {
        it.loadIncludes(true)
        it.registerPass<MyExperimentalPass>()
        it.inferenceConfiguration(
            InferenceConfiguration.builder().inferRecords(true).build()
        )
    }
}
```

## Inspecting the resolved project

`Project.from()` / `project()` return a `Project` object *before* the analysis runs. You can
inspect the resolved configuration — for example, to verify which components were detected — before
calling `analyze()`:

```kotlin
val p = project(Path("/path/to/repo")) { }

println("Components: ${p.components.map { it.name }}")
println("Detection notes:")
p.detectionResults.flatMap { it.notes }.forEach { println("  $it") }

val result = p.analyze()
```

[1]: ../../API/de.fraunhofer.aisec.cpg.project/-directory-component-detector/index.html
[2]: ../../API/de.fraunhofer.aisec.cpg.project/-project-detector/index.html
