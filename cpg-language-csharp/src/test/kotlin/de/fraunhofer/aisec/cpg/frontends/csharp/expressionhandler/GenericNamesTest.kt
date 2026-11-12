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
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * C# allows a type wherever an expression is expected, so a generic name such as `Create<int>` can
 * appear in the expression handler. It becomes a [Reference] to the bare identifier, since the type
 * arguments are not part of the name of the declaration it refers to.
 */
class GenericNamesTest : BaseTest() {

    @Test
    fun testCallGenericMethod() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("GenericNames.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val genericNames = tu.records["Test.GenericNames"]
        assertNotNull(genericNames)

        val method = genericNames.methods["CallGenericMethod"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return Create<int>(x);
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val call = ret.returnValue
        assertIs<Call>(call)

        // the type argument is dropped, so the callee is named `Create` and not `Create<int>`
        val callee = call.callee
        assertIs<Reference>(callee)
        assertLocalName("Create", callee)
        assertInvokes(call, genericNames.methods["Create"])
    }

    @Test
    fun testAccessStaticOfGenericType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("GenericNames.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val genericNames = tu.records["Test.GenericNames"]
        assertNotNull(genericNames)

        val method = genericNames.methods["AccessStaticOfGenericType"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return Holder<int>.Count;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val memberAccess = ret.returnValue
        assertIs<MemberAccess>(memberAccess)
        assertLocalName("Count", memberAccess)

        // the type arguments are dropped here as well, so the base is named `Holder` and resolves
        // to the record. `Count` itself does not resolve yet, because a reference to a type does
        // not carry that type, which is what a member access resolves its member against.
        val base = memberAccess.base
        assertIs<Reference>(base)
        assertLocalName("Holder", base)
        assertRefersTo(base, tu.records["Test.Holder"])
    }

    @Test
    fun testGenericFieldType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("GenericNames.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val genericNames = tu.records["Test.GenericNames"]
        assertNotNull(genericNames)

        // Holder<int> holder;
        val holder = genericNames.fields["holder"]
        assertNotNull(holder)

        // the type argument is not part of the name, so the type is named `Holder` and carries
        // `int` as its generic. This is what lets it be matched with the record.
        val type = holder.type
        assertIs<ObjectType>(type)
        assertLocalName("Holder", type)
        assertEquals(listOf("int"), type.generics.map { it.name.localName })
        assertEquals(tu.records["Test.Holder"], type.recordDeclaration)
    }

    @Test
    fun testQualifiedGenericFieldType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("GenericNames.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val genericNames = tu.records["Test.GenericNames"]
        assertNotNull(genericNames)

        // System.Collections.Generic.List<int> list;
        val list = genericNames.fields["list"]
        assertNotNull(list)

        // only the last part of the dotted name can carry type arguments, so the qualifier becomes
        // the parent of the name and just the `<int>` is moved out of it
        val type = list.type
        assertIs<ObjectType>(type)
        assertLocalName("List", type)
        assertFullName("System.Collections.Generic.List", type)
        assertEquals("System.Collections.Generic", type.name.parent?.toString())
        assertEquals(listOf("int"), type.generics.map { it.name.localName })
    }

    @Test
    fun testAccessThroughGenericType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("GenericNames.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val genericNames = tu.records["Test.GenericNames"]
        assertNotNull(genericNames)

        val method = genericNames.methods["AccessThroughGenericType"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return h.Size;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val memberAccess = ret.returnValue
        assertIs<MemberAccess>(memberAccess)

        // the member resolves, because the type of `h` is named `Holder` and therefore matches the
        // record. Had we kept `<int>` in the name, an inferred record `Holder<int>` without any
        // members would have been created instead.
        val holder = tu.records["Test.Holder"]
        assertNotNull(holder)
        assertRefersTo(memberAccess, holder.fields["Size"])
    }
}
