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
package de.fraunhofer.aisec.cpg.frontends.csharp.expressionhandler

import de.fraunhofer.aisec.cpg.frontends.csharp.CSharpLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * `base` refers to the current instance viewed as its base class. We translate it to a [Reference]
 * to the receiver of the enclosing method, which keeps the data flow through it intact, and give it
 * the type of the base class, which is what a member access on it needs in order to look the member
 * up in the base class instead of the current one.
 */
class BaseAccessTest : BaseTest() {

    @Test
    fun testCallBaseMethod() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("BaseAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val superRecord = tu.records["Test.Super"]
        assertNotNull(superRecord)
        val subRecord = tu.records["Test.Sub"]
        assertNotNull(subRecord)

        val method = subRecord.methods["CallBaseMethod"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return base.Describe();
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val call = ret.returnValue
        assertIs<MemberCall>(call)
        assertLocalName("Describe", call)

        val callee = call.callee
        assertIs<MemberAccess>(callee)
        val base = callee.base
        assertIs<Reference>(base)
        assertLocalName("base", base)

        // `base` is the current instance, so it refers to the receiver of the enclosing method
        assertRefersTo(base, method.receiver)

        // the type of `base` is the base class, so the call resolves to the base class' method and
        // not to the override in `Sub`
        assertLocalName("Super", base.type)
        assertInvokes(call, superRecord.methods["Describe"])

        // the receiver itself keeps the type of the current class, since it is shared with `this`
        assertLocalName("Sub", method.receiver?.type)
    }

    @Test
    fun testAccessBaseField() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("BaseAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val super_ = tu.records["Test.Super"]
        assertNotNull(super_)
        val sub = tu.records["Test.Sub"]
        assertNotNull(sub)

        val method = sub.methods["AccessBaseField"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return base.field;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val memberAccess = ret.returnValue
        assertIs<MemberAccess>(memberAccess)
        assertLocalName("field", memberAccess)
        assertRefersTo(memberAccess, super_.fields["field"])

        val base = memberAccess.base
        assertIs<Reference>(base)
        assertLocalName("base", base)
        assertRefersTo(base, method.receiver)
        assertLocalName("Super", base.type)

        // the field is the one declared in `Super`, so no field is inferred on `Sub` for `base`
        assertTrue(sub.fields.none { it.isInferred })
    }
}
