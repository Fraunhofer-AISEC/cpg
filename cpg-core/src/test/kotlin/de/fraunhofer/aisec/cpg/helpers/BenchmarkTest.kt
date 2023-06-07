/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers

import java.io.File
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class BenchmarkTest {

    @Test
    fun testRelativeOrAbsolute() {
        val relPath = Path("./main.c")
        val absPath = Path("/root/main.c")
        val topLevelRoot = File("/root")
        val topLevelFoo = File("/foo")

        assertEquals(Path("./main.c"), relativeOrAbsolute(relPath, topLevelRoot))
        assertEquals(Path("main.c"), relativeOrAbsolute(absPath, topLevelRoot))

        // This is not what you would expect.
        // But as topLevel is always the largest common path of all source files, this should not
        // happen
        assertEquals(Path("../root/main.c"), relativeOrAbsolute(absPath, topLevelFoo))

        assertEquals(relPath, relativeOrAbsolute(relPath, null))
        assertEquals(absPath, relativeOrAbsolute(absPath, null))
    }
}
