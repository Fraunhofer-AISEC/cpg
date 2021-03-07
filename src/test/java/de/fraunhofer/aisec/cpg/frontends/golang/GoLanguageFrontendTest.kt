package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GoLanguageFrontendTest : BaseTest() {

    @Test
    fun testLiteral() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("literal.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val a = p.getDeclarationsByName("a", VariableDeclaration::class.java).iterator().next()
        assertNotNull(a)

        assertEquals("a", a.name)
        assertEquals(TypeParser.createFrom("int", false), a.type)

        val s = p.getDeclarationsByName("s", VariableDeclaration::class.java).iterator().next()

        assertEquals("s", s.name)
        assertEquals(TypeParser.createFrom("string", false), s.type)

        val f = p.getDeclarationsByName("f", VariableDeclaration::class.java).iterator().next()

        assertEquals("f", f.name)
        assertEquals(TypeParser.createFrom("float64", false), f.type)

        val f32 = p.getDeclarationsByName("f32", VariableDeclaration::class.java).iterator().next()

        assertEquals("f32", f32.name)
        assertEquals(TypeParser.createFrom("float32", false), f32.type)
    }

    @Test
    fun testFunctionDeclaration() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("function.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val main = p.declarations.first() as? FunctionDeclaration
        assertNotNull(main)

        val myTest = p.declarations[1] as? FunctionDeclaration
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

        assertEquals(1, lhs.value)

        val rhs = op.rhs as? Literal<*>
        assertNotNull(rhs)

        assertEquals(2, rhs.value)
    }

    @Test
    fun testStruct() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("struct.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val myStruct = p.getDeclarationsByName("MyStruct", RecordDeclaration::class.java).iterator().next()

        assertNotNull(myStruct)
        assertEquals("struct", myStruct.kind)

        val fields = myStruct.fields

        assertEquals(1, fields.size)

        var methods = myStruct.methods

        var myFunc = methods.first()

        assertEquals("MyFunc", myFunc.name)

        val myField = fields.first()

        assertEquals("MyField", myField.name)
        assertEquals(TypeParser.createFrom("int", false), myField.type)

        val myInterface = p.getDeclarationsByName("MyInterface", RecordDeclaration::class.java).iterator().next()

        assertNotNull(myInterface)
        assertEquals("interface", myInterface.kind)

        methods = myInterface.methods

        assertEquals(1, methods.size)

        myFunc = methods.first()

        assertEquals("MyFunc", myFunc.name)
        assertEquals(TypeParser.createFrom("string", false), myFunc.type)
    }
}