/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewExpression
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShortcutsTest {
    @Test
    fun testCalls() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/ShortcutClass.java"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val actual = result.calls

        val expected = mutableListOf<CallExpression>()
        val classDecl =
            result.translationUnits.firstOrNull()?.declarations?.firstOrNull() as RecordDeclaration
        val main = classDecl.byNameOrNull<MethodDeclaration>("main")
        assertNotNull(main)
        expected.add(
            ((((main.body as CompoundStatement).statements[0] as DeclarationStatement)
                        .declarations[0]
                        as VariableDeclaration)
                    .initializer as NewExpression)
                .initializer as ConstructExpression
        )
        expected.add((main.body as CompoundStatement).statements[1] as MemberCallExpression)
        expected.add((main.body as CompoundStatement).statements[2] as MemberCallExpression)

        val print = classDecl.byNameOrNull<MethodDeclaration>("print")
        assertNotNull(print)
        expected.add(print.bodyOrNull(0)!!)
        expected.add(print.bodyOrNull<CallExpression>(0)?.arguments?.get(0) as CallExpression)

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))

        assertEquals(
            listOf((main.body as CompoundStatement).statements[1] as MemberCallExpression),
            expected.filterByName("print")
        )
    }

    @Test
    fun testCallsByName() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/ShortcutClass.java"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val actual = result.callsByName("print")

        val expected = mutableListOf<CallExpression>()
        val classDecl =
            result.translationUnits.firstOrNull()?.declarations?.firstOrNull() as RecordDeclaration
        val main = classDecl.byNameOrNull<MethodDeclaration>("main")
        assertNotNull(main)
        expected.add((main.body as CompoundStatement).statements[1] as MemberCallExpression)
        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testCalleesOf() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/ShortcutClass.java"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val expected = mutableListOf<FunctionDeclaration>()
        val classDecl =
            result.translationUnits.firstOrNull()?.declarations?.firstOrNull() as RecordDeclaration

        val print = classDecl.byNameOrNull<MethodDeclaration>("print")
        assertNotNull(print)
        expected.add(print)

        val magic = classDecl.byNameOrNull<MethodDeclaration>("magic")
        assertNotNull(magic)
        expected.add(magic)

        val main = classDecl.byNameOrNull<MethodDeclaration>("main")
        assertNotNull(main)
        val actual = main.callees

        expected.add(
            (((((main.body as CompoundStatement).statements[0] as DeclarationStatement)
                            .declarations[0]
                            as VariableDeclaration)
                        .initializer as NewExpression)
                    .initializer as ConstructExpression)
                .constructor!!
        )

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testCallersOf() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/ShortcutClass.java"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val classDecl =
            result.translationUnits.firstOrNull()?.declarations?.firstOrNull() as RecordDeclaration
        val print = classDecl.byNameOrNull<MethodDeclaration>("print")
        assertNotNull(print)

        val actual = result.callersOf(print)

        val expected = mutableListOf<FunctionDeclaration>()
        val main = classDecl.byNameOrNull<MethodDeclaration>("main")
        assertNotNull(main)
        expected.add(main)
        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testControls() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/ShortcutClass.java"))
                .defaultPasses()
                .defaultLanguages()
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val expected = mutableListOf<Node>()
        val classDecl =
            result.translationUnits.firstOrNull()?.declarations?.firstOrNull() as RecordDeclaration
        val magic = classDecl.byNameOrNull<MethodDeclaration>("magic")
        assertNotNull(magic)
        val ifStatement = (magic.body as CompoundStatement).statements[0] as IfStatement

        val actual = ifStatement.controls()
        expected.add(ifStatement.thenStatement)
        val thenStatement =
            (ifStatement.thenStatement as CompoundStatement).statements[0] as IfStatement
        expected.add(thenStatement)
        expected.add(thenStatement.condition)
        expected.add((thenStatement.condition as BinaryOperator).lhs)
        expected.add(((thenStatement.condition as BinaryOperator).lhs as MemberExpression).base)
        expected.add((thenStatement.condition as BinaryOperator).rhs)
        val nestedThen = thenStatement.thenStatement as CompoundStatement
        expected.add(nestedThen)
        expected.add(nestedThen.statements[0])
        expected.add((nestedThen.statements[0] as BinaryOperator).lhs)
        expected.add(((nestedThen.statements[0] as BinaryOperator).lhs as MemberExpression).base)
        expected.add((nestedThen.statements[0] as BinaryOperator).rhs)
        val nestedElse = thenStatement.elseStatement as CompoundStatement
        expected.add(nestedElse)
        expected.add(nestedElse.statements[0])
        expected.add((nestedElse.statements[0] as BinaryOperator).lhs)
        expected.add(((nestedElse.statements[0] as BinaryOperator).lhs as MemberExpression).base)
        expected.add((nestedElse.statements[0] as BinaryOperator).rhs)

        expected.add(ifStatement.elseStatement)
        expected.add((ifStatement.elseStatement as CompoundStatement).statements[0])
        expected.add(
            ((ifStatement.elseStatement as CompoundStatement).statements[0] as BinaryOperator).lhs
        )
        expected.add(
            (((ifStatement.elseStatement as CompoundStatement).statements[0] as BinaryOperator).lhs
                    as MemberExpression)
                .base
        )
        expected.add(
            ((ifStatement.elseStatement as CompoundStatement).statements[0] as BinaryOperator).rhs
        )

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testControlledBy() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/ShortcutClass.java"))
                .defaultPasses()
                .defaultLanguages()
                .registerPass(EdgeCachePass())
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        val expected = mutableListOf<Node>()
        val classDecl =
            result.translationUnits.firstOrNull()?.declarations?.firstOrNull() as RecordDeclaration
        val magic = classDecl.byNameOrNull<MethodDeclaration>("magic")
        assertNotNull(magic)

        // get the statement attr = 3;
        val ifStatement = (magic.body as CompoundStatement).statements[0] as IfStatement
        val thenStatement =
            (ifStatement.thenStatement as CompoundStatement).statements[0] as IfStatement
        val nestedThen = thenStatement.thenStatement as CompoundStatement
        val interestingNode = nestedThen.statements[0]
        val actual = interestingNode.controlledBy()

        expected.add(ifStatement)
        expected.add(thenStatement)

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }
}
