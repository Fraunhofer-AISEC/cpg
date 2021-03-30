package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.ExperimentalPython
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import net.bytebuddy.pool.TypePool
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import de.fraunhofer.aisec.cpg.sarif.Region
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.net.URI

@ExperimentalPython
class PythonFrontendTest : BaseTest() {
    // TODO ensure gradle doesn't remove those classes
    val dummyRegion = Region()
    val dummyPhysicalLocation = PhysicalLocation(URI(""), dummyRegion)

    @Test
    fun testLiteral() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("literal.py").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("literal", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)
        assertEquals("literal", p.name)

        val b = p.getDeclarationsByName("b", VariableDeclaration::class.java).iterator().next()
        assertNotNull(b)
        assertEquals("b", b.name)
        assertEquals(TypeParser.createFrom("bool", false), b.type)

        val i = p.getDeclarationsByName("i", VariableDeclaration::class.java).iterator().next()
        assertNotNull(i)
        assertEquals("i", i.name)
        assertEquals(TypeParser.createFrom("int", false), i.type)

        val f = p.getDeclarationsByName("f", VariableDeclaration::class.java).iterator().next()
        assertNotNull(f)
        assertEquals("f", f.name)
        assertEquals(TypeParser.createFrom("float", false), f.type)

        /*
        val c = p.getDeclarationsByName("c", VariableDeclaration::class.java).iterator().next()
        assertNotNull(c)
        assertEquals("c", c.name)
        assertEquals(TypeParser.createFrom("complex", false), c.type)
        */

        val t = p.getDeclarationsByName("t", VariableDeclaration::class.java).iterator().next()
        assertNotNull(t)
        assertEquals("t", t.name)
        assertEquals(TypeParser.createFrom("str", false), t.type)

        val n = p.getDeclarationsByName("n", VariableDeclaration::class.java).iterator().next()
        assertNotNull(n)
        assertEquals("n", n.name)
        assertEquals(TypeParser.createFrom("None", false), n.type)
    }

    @Test
    fun testFunctionDeclaration() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("function.py").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("function", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val foo = p.declarations.first() as? FunctionDeclaration
        assertNotNull(foo)

        val bar = p.declarations[1] as? FunctionDeclaration
        assertNotNull(bar)
        assertEquals(1, bar.parameters.size)

        var body = foo.body as? CompoundStatement
        assertNotNull(body)

        var callExpression = body.statements.first() as? CallExpression
        assertNotNull(callExpression)

        assertEquals("bar", callExpression.name)
        assertEquals(bar, callExpression.invokes.iterator().next())

        val s = bar.parameters.first()
        assertNotNull(s)
        assertEquals("s", s.name)
        assertEquals(TypeParser.createFrom("str", false), s.type)

        assertEquals("bar", bar.name)

        body = bar.body as? CompoundStatement
        assertNotNull(body)

        callExpression = body.statements.first() as? CallExpression
        assertNotNull(callExpression)

        assertEquals("print", callExpression.fqn)

        val literal = callExpression.arguments.first() as? Literal<*>
        assertNotNull(literal)

        assertEquals("bar(s) here: ", literal.value)
        assertEquals(TypeParser.createFrom("str", false), literal.type)

        val ref = callExpression.arguments[1] as? DeclaredReferenceExpression
        assertNotNull(ref)

        assertEquals("s", ref.name)
        assertEquals(s, ref.refersTo)

        val stmt = body.statements[1] as? DeclarationStatement
        assertNotNull(stmt)

        val a = stmt.singleDeclaration as? VariableDeclaration
        assertNotNull(a)

        assertEquals("a", a.name)

        val op = a.initializer as? BinaryOperator
        assertNotNull(op)

        assertEquals("+", op.operatorCode)

        val lhs = op.lhs as? Literal<*>
        assertNotNull(lhs)

        assertEquals(1, (lhs.value as? Long)?.toInt())

        val rhs = op.rhs as? Literal<*>
        assertNotNull(rhs)

        assertEquals(2, (rhs.value as? Long)?.toInt())

        val r = body.statements[2] as? ReturnStatement

        assertNotNull(r)

    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("if.py").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("if", NamespaceDeclaration::class.java).iterator().next()

        val main = p.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)

        val body = main.body as? CompoundStatement

        assertNotNull(body)

        val sel = (body.statements.first() as? DeclarationStatement)?.singleDeclaration as? VariableDeclaration

        assertNotNull(sel)
        assertEquals("sel", sel.name)
        assertEquals(TypeParser.createFrom("bool", false), sel.type)

        val initializer = sel.initializer as? Literal<*>

        assertNotNull(initializer)
        assertEquals(TypeParser.createFrom("bool", false), initializer.type)
        assertEquals("True", initializer.name)

        val `if` = body.statements[1] as? IfStatement
        assertNotNull(`if`)
    }
}