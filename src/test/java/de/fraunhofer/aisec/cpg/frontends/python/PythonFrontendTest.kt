package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.ExperimentalPython
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@ExperimentalPython
@Tag("experimental")
class PythonFrontendTest : BaseTest() {
    // TODO ensure gradle doesn't remove those classes
    val dummyRegion = Region()
    val dummyPhysicalLocation = PhysicalLocation(URI(""), dummyRegion)

    @Test
    fun testLiteral() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("literal.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("literal", NamespaceDeclaration::class.java).iterator().next()
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
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("function.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("function", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val foo = p.declarations.first() as? FunctionDeclaration
        assertNotNull(foo)

        val bar = p.declarations[1] as? FunctionDeclaration
        assertNotNull(bar)
        assertEquals(2, bar.parameters.size)

        var body = foo.body as? CompoundStatement
        assertNotNull(body)

        var callExpression = body.statements.first() as? CallExpression
        assertNotNull(callExpression)

        assertEquals("bar", callExpression.name)
        assertEquals(bar, callExpression.invokes.iterator().next())

        val edge = callExpression.argumentsEdges[1]

        assertEquals("s2", edge.getProperty(Properties.NAME))

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
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("if.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("if", NamespaceDeclaration::class.java).iterator().next()

        val main = p.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)

        val body = main.body as? CompoundStatement

        assertNotNull(body)

        val sel =
            (body.statements.first() as? DeclarationStatement)?.singleDeclaration as?
                VariableDeclaration

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

    @Test
    fun testSimpleClass() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("simple_class.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)
        val p =
            tu.getDeclarationsByName("simple_class", NamespaceDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(p)

        val cls =
            p.getDeclarationsByName("SomeClass", RecordDeclaration::class.java).iterator().next()
        assertNotNull(cls)
        val foo = p.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(foo)

        assertEquals("SomeClass", cls.name)
        assertEquals(1, cls.methods.size)
        assertEquals(1, cls.constructors.size) // auto generated by cpg
        assertEquals(true, cls.constructors.first().isImplicit)

        val clsfunc = cls.methods.first()
        assertEquals("someFunc", clsfunc.name)

        assertEquals("foo", foo.name)
        val body = foo.body as? CompoundStatement
        assertEquals(2, body?.statements?.size)

        val s1 = body?.statements?.get(0) as? DeclarationStatement
        val s2 = body?.statements?.get(1) as? MemberCallExpression

        val c1 = s1?.declarations?.get(0) as? VariableDeclaration
        assertEquals("c1", c1?.name)
        val ctor = (c1?.initializer as? ConstructExpression)?.constructor
        assertEquals(ctor, cls.constructors.first())
        assertEquals(TypeParser.createFrom("SomeClass", false), c1?.type)

        assertEquals(c1, (s2?.base as? DeclaredReferenceExpression)?.refersTo)
        assertEquals(1, s2?.invokes?.size)
        assertEquals(clsfunc, s2?.invokes?.first())

        // member
    }

    @Test
    fun testIfExpr() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ifexpr.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("ifexpr", NamespaceDeclaration::class.java).iterator().next()

        val main = p.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)

        val body = main.body as? CompoundStatement

        assertNotNull(body)

        val foo =
            (body.statements.first() as? DeclarationStatement)?.singleDeclaration as?
                VariableDeclaration

        assertNotNull(foo)
        assertEquals("foo", foo.name)
        assertEquals(TypeParser.createFrom("int", false), foo.type)

        val initializer = foo.initializer as? ConditionalExpression

        assertNotNull(initializer)
        assertEquals(TypeParser.createFrom("int", false), initializer.type)

        val ifCond = initializer.condition as? Literal<*>
        val thenExpr = initializer.thenExpr as? Literal<*>
        val elseExpr = initializer.elseExpr as? Literal<*>

        assertEquals(TypeParser.createFrom("bool", false), ifCond?.type)
        assertEquals(false, ifCond?.value)

        assertEquals(TypeParser.createFrom("int", false), thenExpr?.type)
        assertEquals(21, (thenExpr?.value as? Long)?.toInt())

        assertEquals(TypeParser.createFrom("int", false), elseExpr?.type)
        assertEquals(42, (elseExpr?.value as? Long)?.toInt())
    }
}
