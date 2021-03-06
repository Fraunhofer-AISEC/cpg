package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.List
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GoLanguageFrontendTest : BaseTest() {

    @Test
    fun testLiteral() {
        val topLevel =Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("literal.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val a = tu.declarations.first() as? VariableDeclaration
        assertNotNull(a)

        assertEquals("a", a.name)


    }

    @Test
    fun testFunctionDeclaration() {
        val topLevel =Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("function.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val main = tu.declarations.first() as? FunctionDeclaration
        assertNotNull(main)

        val myTest = tu.declarations[1] as? FunctionDeclaration
        assertNotNull(myTest)
        assertEquals(1, myTest.parameters.size)

        var body = main.body as? CompoundStatement
        assertNotNull(body)

        var callExpression = body.statements.first() as? CallExpression
        assertNotNull(callExpression)

        assertEquals("myTest", callExpression.name)
        assertEquals(myTest, callExpression.invokes.iterator().next())

        val s = myTest.parameters.first()
        assertNotNull(s)
        assertEquals("s", s.name)
        assertEquals(TypeParser.createFrom("string", false), s.type)

        assertEquals("myTest", myTest.name)

        body = myTest.body as? CompoundStatement
        assertNotNull(body)

        callExpression = body.statements.first() as? CallExpression
        assertNotNull(callExpression)

        assertEquals("fmt.Printf", callExpression.name)

        val literal = callExpression.arguments.first() as? Literal<*>
        assertNotNull(literal)

        assertEquals("%s", literal.value)
        assertEquals(TypeParser.createFrom("string", false), literal.type)

        val ref = callExpression.arguments[1] as? DeclaredReferenceExpression
        assertNotNull(ref)

        assertEquals("s", ref.name)
        assertEquals(s, ref.refersTo)
    }
}