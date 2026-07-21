/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.project

import java.nio.file.Path

/** The operating system a [Project] is targeting. */
enum class OperatingSystem {
    LINUX,
    MACOS,
    WINDOWS,
    FREEBSD,
    UNKNOWN;

    companion object {
        /** Determines the [OperatingSystem] of the current host. */
        fun host(): OperatingSystem {
            val name = System.getProperty("os.name").lowercase()
            return when {
                name.contains("linux") -> LINUX
                name.contains("mac") -> MACOS
                name.contains("windows") -> WINDOWS
                name.contains("freebsd") -> FREEBSD
                else -> UNKNOWN
            }
        }
    }
}

/** The processor architecture a [Project] is targeting. */
enum class Architecture(
    /** The pointer width (in bits) of this architecture. */
    val bits: Int
) {
    X86_64(64),
    ARM64(64),
    X86(32),
    ARM(32),
    RISCV64(64),
    UNKNOWN(64);

    companion object {
        /** Determines the [Architecture] of the current host. */
        fun host(): Architecture {
            return when (System.getProperty("os.arch").lowercase()) {
                "x86_64",
                "amd64" -> X86_64
                "aarch64",
                "arm64" -> ARM64
                "x86",
                "i386",
                "i686" -> X86
                "arm" -> ARM
                "riscv64" -> RISCV64
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Describes the external environment a [Project] is running on (or being built for). This
 * information can be used by language frontends to configure language-specific behaviour, for
 * example:
 * - The C/C++ frontend can derive built-in preprocessor macros (such as `__linux__` or
 *   `__aarch64__`) as well as the width of pointer and integer types from [os] and [architecture].
 * - The Go frontend can derive the `GOOS` and `GOARCH` build constraints from it.
 *
 * By default, the environment of the current host (see [host]) is assumed. It can be overridden to
 * analyze a project for a different target than the machine the analysis runs on (cross-compilation
 * style analysis).
 */
class TargetEnvironment(
    /** The target operating system. */
    val os: OperatingSystem = OperatingSystem.host(),
    /** The target processor architecture. */
    val architecture: Architecture = Architecture.host(),
    /** Environment variables that are assumed to be present in the target environment. */
    val env: Map<String, String> = mapOf(),
    /**
     * An optional system root containing headers and libraries of the target environment, mainly
     * useful for C/C++.
     */
    val sysroot: Path? = null,
) {
    companion object {
        /** Creates a [TargetEnvironment] that mirrors the current host. */
        fun host(): TargetEnvironment {
            return TargetEnvironment()
        }
    }
}

/** A builder DSL for [TargetEnvironment], used by [ProjectBuilder.environment]. */
class TargetEnvironmentBuilder {
    var os: OperatingSystem = OperatingSystem.host()
    var architecture: Architecture = Architecture.host()
    var sysroot: Path? = null
    private val env = mutableMapOf<String, String>()

    /** Adds an environment variable to the target environment. */
    fun env(key: String, value: String) {
        env[key] = value
    }

    fun build(): TargetEnvironment {
        return TargetEnvironment(os = os, architecture = architecture, env = env, sysroot = sysroot)
    }
}
