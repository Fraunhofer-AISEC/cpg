/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.golang

import kotlin.test.*

// TODO for merge
@Ignore
class BuildConstraintsTest {

    @Test
    fun testFromString() {
        val string = "darwin && !amd64"

        val root = BuildConstraintExpression.fromString(string)
        assertNotNull(root)

        var expr = root
        assertIs<BuildConstraintBinaryExpression>(expr)
        assertEquals("&&", expr.operatorCode)

        val lhs = expr.lhs
        assertIs<BuildConstraintTag>(lhs)
        assertEquals("darwin", lhs.tag)

        val rhs = expr.rhs
        assertIs<BuildConstraintUnaryExpression>(rhs)
        assertEquals("!", rhs.operatorCode)

        expr = rhs.expr
        assertIs<BuildConstraintTag>(expr)
        assertEquals("amd64", expr.tag)

        val darwinAmd64 = setOf("darwin", "amd64")
        assertFalse(root.evaluate(darwinAmd64))

        val darwinArm64 = setOf("darwin", "arm64")
        assertTrue(root.evaluate(darwinArm64))
    }
}
