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
package de.fraunhofer.aisec.cpg.frontends.jvm

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileIdentificationTest {
    @Test
    fun testRealApk() {
        val topLevel = Path.of("src", "test", "resources", "apk", "HelloWorld")
        val apkFile = topLevel.resolve("real-app-debug.apk").toFile()
        assertTrue(apkFile.isApk())
        assertFalse(apkFile.isJar())
    }

    @Test
    fun testRealApkNoEnding() {
        val topLevel = Path.of("src", "test", "resources", "apk", "HelloWorld")
        val apkFile = topLevel.resolve("real-app-debug").toFile()
        assertTrue(apkFile.isApk())
        assertFalse(apkFile.isJar())
    }

    @Test
    fun testFakeApk() {
        val topLevel = Path.of("src", "test", "resources", "apk", "HelloWorld")
        val apkFile = topLevel.resolve("app-debug.apk").toFile()
        assertTrue(apkFile.isApk())
        assertTrue(apkFile.isJar())
    }

    @Test
    fun testJar() {
        val topLevel = Path.of("src", "test", "resources", "jar", "literals")
        val apkFile = topLevel.resolve("literals.jar").toFile()
        assertFalse(apkFile.isApk())
        assertTrue(apkFile.isJar())
    }

    @Test
    fun testJarNoEnding() {
        val topLevel = Path.of("src", "test", "resources", "jar", "literals")
        val apkFile = topLevel.resolve("literals").toFile()
        assertFalse(apkFile.isApk())
        assertTrue(apkFile.isJar())
    }
}
