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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertFullName
import de.fraunhofer.aisec.cpg.test.assertNotRefersTo
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import java.io.File
import kotlin.test.*

class SymbolResolverTest {
    @Test
    fun testFields() {
        val topLevel = File("src/test/resources/python/fields.py")
        val result =
            analyze(listOf(topLevel), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
            }

        val globalA =
            result.namespaces["fields"]
                .variables[{ it.name.localName == "a" && it !is FieldDeclaration }]
        assertNotNull(globalA)

        // Make sure, we only have one (!) field a
        val fieldsA = result.records["MyClass"]?.fields("a")
        val fieldA = fieldsA?.singleOrNull()
        assertNotNull(fieldA)

        val aRefs = result.refs("a")
        aRefs.filterIsInstance<MemberExpression>().forEach { assertRefersTo(it, fieldA) }
        aRefs.filter { it !is MemberExpression }.forEach { assertRefersTo(it, globalA) }

        // We should only have one reference to "os" -> the member expression "self.os"
        val osRefs = result.refs("os")
        assertEquals(1, osRefs.size)
        assertIs<MemberExpression>(osRefs.singleOrNull())

        // "os.name" is not a member expression but a reference to the field "name" of the "os"
        // module, therefore it is a reference
        val osNameRefs = result.refs("os.name")
        assertEquals(1, osNameRefs.size)
        assertIsNot<MemberExpression>(osNameRefs.singleOrNull())

        // Same tests but for fields declared at the record level.
        // A variable "declared" inside a class is considered a field in Python.
        val fieldCopyA = result.records["MyClass"]?.fields["copyA"]
        assertIs<FieldDeclaration>(fieldCopyA)
        val baz = result.records["MyClass"]?.methods["baz"]
        assertIs<MethodDeclaration>(baz)
        val bazPrint = baz.calls("print").singleOrNull()
        assertIs<CallExpression>(bazPrint)
        val bazPrintArgument = bazPrint.arguments.firstOrNull()
        assertRefersTo(bazPrintArgument, fieldCopyA)

        // make sure, that this does not work without the receiver
        val bazDoesNotWork = baz.calls("doesNotWork").singleOrNull()
        assertIs<CallExpression>(bazDoesNotWork)
        val bazDoesNotWorkArgument = bazDoesNotWork.arguments.firstOrNull()
        assertNotNull(bazDoesNotWorkArgument)
        assertNotRefersTo(bazDoesNotWorkArgument, fieldCopyA)
    }

    @Test
    fun testParentClassConfusion() {
        val topLevel = File("src/test/resources/python")
        val result =
            analyze(
                listOf(topLevel.resolve("parent_confusion/package/myclass.py")),
                topLevel.toPath(),
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val myClass = result.records["MyClass"]
        assertNotNull(myClass)
        assertFullName("other.myclass.MyClass", myClass.superTypes.singleOrNull())
    }

    @Test
    fun testCallVsMemberCallConfusion() {
        val topLevel = File("src/test/resources/python")
        val result =
            analyze(listOf(topLevel.resolve("call_confusion.py")), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val someContext = result.records["context.SomeContext"]
        assertNotNull(someContext)
        assertTrue(someContext.isInferred, "Expected to infer a record 'context.SomeContext'")

        val doSomething = result.calls("do_something").singleOrNull()
        assertNotNull(doSomething, "Expected to find a single call to 'do_something'")
        assertIs<MemberCallExpression>(doSomething, "'do_something' should be a member call")
    }
}
