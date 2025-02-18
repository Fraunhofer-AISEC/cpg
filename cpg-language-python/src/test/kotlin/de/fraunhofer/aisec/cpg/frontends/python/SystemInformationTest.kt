/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.python

import kotlin.test.Test
import kotlin.test.assertTrue

class SystemInformationTest {
    @Test
    fun testVersionInfo() {
        val empty = VersionInfo()
        val v123_2 = VersionInfo(1, 2, 3)
        val v123 = VersionInfo(1, 2, 3)
        val v321 = VersionInfo(3, 2, 1)
        val v121 = VersionInfo(1, 2, 1)
        val v132 = VersionInfo(1, 3, 2)
        val v124 = VersionInfo(1, 2, 4)
        assertTrue(empty < v123)
        assertTrue(v123_2.compareTo(v123) == 0)
        assertTrue(v123 > empty)
        assertTrue(v123 < v321)
        assertTrue(v121 < v123)
        assertTrue(v123 < v132)
        assertTrue(v123 < v124)

        assertTrue(v321 > v123)
        assertTrue(v123 > v121)
        assertTrue(v132 > v123)
        assertTrue(v124 > v123)
    }
}
