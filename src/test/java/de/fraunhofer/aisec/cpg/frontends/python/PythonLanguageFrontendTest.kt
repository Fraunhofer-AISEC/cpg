/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  $$$$$$\  $$$$$$$\   $$$$$$\
 * $$  __$$\ $$  __$$\ $$  __$$\
 * $$ /  \__|$$ |  $$ |$$ /  \__|
 * $$ |      $$$$$$$  |$$ |$$$$\
 * $$ |      $$  ____/ $$ |\_$$ |
 * $$ |  $$\ $$ |      $$ |  $$ |
 * \$$$$$   |$$ |      \$$$$$   |
 *  \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import org.junit.jupiter.api.BeforeEach
import kotlin.Throws
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import java.io.File
import org.junit.jupiter.api.Assertions
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class PythonLanguageFrontendTest {
    private var config: TranslationConfiguration? = null
    @BeforeEach
    fun setUp() {
        config = TranslationConfiguration.builder().build()
    }

    @Test
    @Throws(TranslationException::class)
    fun testSimple() {
        val declaration = PythonLanguageFrontend(config, ScopeManager())
            .parse(File("src/test/resources/main.py"))
        assertNotNull(declaration)
        val declarations = declaration.declarations
        assertEquals(1, declarations.size)

        // first declaration is the function declaration
        assertTrue(declarations[0] is FunctionDeclaration)

        val functionDeclaration = declarations[0] as FunctionDeclaration
        assertEquals("test", functionDeclaration.name)

        val body = functionDeclaration.body as CompoundStatement
        val statements = body.statements

        assertEquals(4, statements.size)
        val stmt = statements[0]
        assertTrue(stmt is CallExpression)

        val call = stmt as CallExpression
        assertEquals("print", call.name)

        val binOp = statements[3] as? BinaryOperator

        assertNotNull(binOp)

        val lhs = binOp.lhs as? DeclaredReferenceExpression

        assertNotNull(lhs)

        assertEquals("k", lhs?.name)
    }
}