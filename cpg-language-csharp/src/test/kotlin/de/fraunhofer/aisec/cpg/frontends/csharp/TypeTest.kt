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
package de.fraunhofer.aisec.cpg.frontends.csharp

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.StringType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/** Tests for the types that [CSharpLanguageFrontend.typeOf] builds out of a `TypeSyntax`. */
class TypeTest : BaseTest() {

    @Test
    fun testGenericType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val result =
            analyze(listOf(topLevel.resolve("Inheritance.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        val tu = result.components.firstOrNull()?.translationUnits?.firstOrNull()
        assertNotNull(tu)

        val container = tu.records["GenericClass"]
        assertNotNull(container)
        assertContains(container.modifiers, "public")

        val typeT = result.finalCtx.typeManager.getTypeParameter(container, "T")
        assertNotNull(typeT)
        assertIs<ParameterizedType>(typeT)
    }

    /**
     * The `scoped` modifier promises that a reference does not escape the method it is declared in.
     * That is a lifetime constraint the compiler checks, it has no runtime representation and it
     * does not change which type is being named, so it is dropped. In front of a local variable
     * Roslyn makes it part of the type, which is the
     * [ScopedTypeSyntax][Csharp.AST.ScopedTypeSyntax] we unwrap here, whereas in front of a
     * parameter it is one of the parameter's modifiers and never reaches the type in the first
     * place.
     */
    @Test
    fun testScopedType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ScopedTypes.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["HelloWorld.Scoped"]?.methods["LocalWithScopedType"]
        assertNotNull(method)

        // scoped Buffer buffer = default;
        val buffer = method.variables["buffer"]
        assertNotNull(buffer)
        assertLocalName("Buffer", buffer.type)

        // and because the type is the plain `Buffer` and not something named `scoped Buffer`, the
        // member access through it resolves to the real field
        val body = method.body
        assertIs<Block>(body)
        val returnStmt = body.statements.filterIsInstance<Return>().single()
        val memberAccess = returnStmt.returnValue
        assertIs<MemberAccess>(memberAccess)
        assertRefersTo(memberAccess, tu.records["HelloWorld.Buffer"]?.fields["Length"])
    }

    /**
     * A `?` after a type is two different C# features sharing one syntax node. On a reference type,
     * such as `string?`, it is only an annotation for the compiler's null-state analysis and is
     * erased at runtime. On a value type, such as `int?`, it really means `System.Nullable<int>`.
     * The syntax does not tell the two apart, so both simply become the type without the `?`.
     */
    @Test
    fun testNullableReferenceType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("NullableTypes.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["Test.NullableTypes"]
        assertNotNull(record)

        // string? text;
        val text = record.fields["text"]
        assertNotNull(text)

        // `string?` and `string` are the same type at runtime, so we get the built-in string type
        // and not something named `string?`
        val type = text.type
        assertIs<StringType>(type)
        assertLocalName("string", type)
    }

    /** See [testNullableReferenceType] for the two meanings of a `?` after a type. */
    @Test
    fun testNullableValueType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("NullableTypes.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["Test.NullableTypes"]
        assertNotNull(record)

        // int? number;
        val number = record.fields["number"]
        assertNotNull(number)

        // this is really a `System.Nullable<int>`, but we cannot tell that from the syntax, so we
        // treat it like the nullable reference types above and settle for `int`
        val type = number.type
        assertIs<IntegerType>(type)
        assertLocalName("int", type)
    }

    /** See [testNullableReferenceType] for the two meanings of a `?` after a type. */
    @Test
    fun testNullableArrayType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("NullableTypes.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["Test.NullableTypes"]
        assertNotNull(record)

        // byte[]? bytes;
        val bytes = record.fields["bytes"]
        assertNotNull(bytes)

        // the `?` is peeled off and what remains is handled by the array type below it
        val type = bytes.type
        assertIs<PointerType>(type)
        assertEquals(PointerType.PointerOrigin.ARRAY, type.pointerOrigin)
        assertIs<IntegerType>(type.elementType)
        assertLocalName("byte", type.elementType)
    }

    /** See [testNullableReferenceType] for the two meanings of a `?` after a type. */
    @Test
    fun testNullableGenericType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("NullableTypes.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["Test.NullableTypes"]
        assertNotNull(record)

        // System.Collections.Generic.List<int>? list;
        val list = record.fields["list"]
        assertNotNull(list)

        // peeling off the `?` leaves the qualified generic name, so the type arguments still end up
        // as the generics and the qualifier still ends up as the parent of the name
        val type = list.type
        assertIs<ObjectType>(type)
        assertLocalName("List", type)
        assertFullName("System.Collections.Generic.List", type)
        assertEquals(listOf("int"), type.generics.map { it.name.localName })
    }

    /** See [testNullableReferenceType] for the two meanings of a `?` after a type. */
    @Test
    fun testAccessThroughNullable() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("NullableTypes.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["Test.NullableTypes"]
        assertNotNull(record)

        val method = record.methods["AccessThroughNullable"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return h.Size;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val memberAccess = ret.returnValue
        assertIs<MemberAccess>(memberAccess)

        // the member resolves, because the type of `h` is named `Holder` and therefore matches the
        // record. Had we kept the `?` in the name, an inferred record `Holder?` without any members
        // would have been created instead.
        val holder = tu.records["Test.Holder"]
        assertNotNull(holder)
        assertRefersTo(memberAccess, holder.fields["Size"])
    }
}
