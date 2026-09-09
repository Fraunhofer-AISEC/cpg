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

import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

/** Runs [block] with the `os.name` system property set to [name], then restores it. */
private fun withOsName(name: String, block: () -> Unit) {
    val original = System.getProperty("os.name")
    try {
        System.setProperty("os.name", name)
        block()
    } finally {
        System.setProperty("os.name", original)
    }
}

/** Runs [block] with the `os.arch` system property set to [arch], then restores it. */
private fun withOsArch(arch: String, block: () -> Unit) {
    val original = System.getProperty("os.arch")
    try {
        System.setProperty("os.arch", arch)
        block()
    } finally {
        System.setProperty("os.arch", original)
    }
}

class TargetEnvironmentTest {
    @Test
    fun testOperatingSystemHost() {
        withOsName("Linux") { assertEquals(OperatingSystem.LINUX, OperatingSystem.host()) }
        withOsName("Mac OS X") { assertEquals(OperatingSystem.MACOS, OperatingSystem.host()) }
        withOsName("Windows 11") { assertEquals(OperatingSystem.WINDOWS, OperatingSystem.host()) }
        withOsName("FreeBSD") { assertEquals(OperatingSystem.FREEBSD, OperatingSystem.host()) }
        withOsName("SunOS") { assertEquals(OperatingSystem.UNKNOWN, OperatingSystem.host()) }
    }

    @Test
    fun testArchitectureHost() {
        withOsArch("x86_64") { assertEquals(Architecture.X86_64, Architecture.host()) }
        withOsArch("amd64") { assertEquals(Architecture.X86_64, Architecture.host()) }
        withOsArch("aarch64") { assertEquals(Architecture.ARM64, Architecture.host()) }
        withOsArch("arm64") { assertEquals(Architecture.ARM64, Architecture.host()) }
        withOsArch("x86") { assertEquals(Architecture.X86, Architecture.host()) }
        withOsArch("i386") { assertEquals(Architecture.X86, Architecture.host()) }
        withOsArch("i686") { assertEquals(Architecture.X86, Architecture.host()) }
        withOsArch("arm") { assertEquals(Architecture.ARM, Architecture.host()) }
        withOsArch("riscv64") { assertEquals(Architecture.RISCV64, Architecture.host()) }
        withOsArch("sparc") { assertEquals(Architecture.UNKNOWN, Architecture.host()) }
    }

    @Test
    fun testArchitectureBits() {
        assertEquals(64, Architecture.X86_64.bits)
        assertEquals(64, Architecture.ARM64.bits)
        assertEquals(32, Architecture.X86.bits)
        assertEquals(32, Architecture.ARM.bits)
        assertEquals(64, Architecture.RISCV64.bits)
        assertEquals(64, Architecture.UNKNOWN.bits)
    }

    @Test
    fun testTargetEnvironmentHostMirrorsCurrentHost() {
        val environment = TargetEnvironment.host()

        assertEquals(OperatingSystem.host(), environment.os)
        assertEquals(Architecture.host(), environment.architecture)
    }

    @Test
    fun testTargetEnvironmentBuilder() {
        val environment =
            TargetEnvironmentBuilder()
                .apply {
                    os = OperatingSystem.LINUX
                    architecture = Architecture.ARM64
                    sysroot = Path("/sysroot")
                    env("GOOS", "linux")
                    env("GOARCH", "arm64")
                }
                .build()

        assertEquals(OperatingSystem.LINUX, environment.os)
        assertEquals(Architecture.ARM64, environment.architecture)
        assertEquals(Path("/sysroot"), environment.sysroot)
        assertEquals(mapOf("GOOS" to "linux", "GOARCH" to "arm64"), environment.env)
    }
}
