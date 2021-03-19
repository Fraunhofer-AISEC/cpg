package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import org.checkerframework.checker.nullness.qual.Nullable
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoLanguageFrontendTest : BaseTest() {

    @Test
    fun testConstruct() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("construct.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        assertNotNull(p)

        val myStruct = p.getDeclarationsByName("p.MyStruct", RecordDeclaration::class.java).iterator().next()

        assertNotNull(myStruct)

        val main = p.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)

        val body = main.body as? CompoundStatement

        assertNotNull(body)

        // new

        var stmt = body.statements.first() as? DeclarationStatement

        assertNotNull(stmt)

        var decl = stmt.singleDeclaration as? VariableDeclaration

        assertNotNull(decl)

        val new = decl.initializer as? NewExpression

        assertNotNull(new)

        val construct = new.initializer as? ConstructExpression

        assertNotNull(construct)

        assertEquals(myStruct, construct.instantiates)

        // make array

        stmt = body.statements[1] as? DeclarationStatement

        assertNotNull(stmt)

        decl = stmt.singleDeclaration as? VariableDeclaration

        assertNotNull(decl)

        var make = decl.initializer

        assertNotNull(make)
        assertEquals(TypeParser.createFrom("int[]", false), make.type)

        assertTrue(make is ArrayCreationExpression)

        val dimension = make.dimensions.first() as? Literal<*>

        assertNotNull(dimension)
        assertEquals(5, dimension.value)

        // make map

        stmt = body.statements[2] as? DeclarationStatement

        assertNotNull(stmt)

        decl = stmt.singleDeclaration as? VariableDeclaration

        assertNotNull(decl)

        make = decl.initializer

        assertNotNull(make)
        assertTrue(make is ConstructExpression)
        assertEquals(TypeParser.createFrom("map<string,string>", false), make.type)

        // make channel

        stmt = body.statements[3] as? DeclarationStatement

        assertNotNull(stmt)

        decl = stmt.singleDeclaration as? VariableDeclaration

        assertNotNull(decl)

        make = decl.initializer

        assertNotNull(make)
        assertTrue(make is ConstructExpression)
        assertEquals(TypeParser.createFrom("chan<int>", false), make.type)
    }

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

        assertEquals("fmt.Printf", callExpression.fqn)

        val base = callExpression.base as? DeclaredReferenceExpression

        assertNotNull(base)

        val include = base.refersTo as? IncludeDeclaration

        assertNotNull(include)
        assertEquals("fmt", include.name)

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

        val binOp = body.statements[2] as? BinaryOperator

        assertNotNull(binOp)

        val err = binOp.lhs

        assertNotNull(err)
        assertEquals(TypeParser.createFrom("error", false), err.type)
    }

    @Test
    fun testStruct() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("struct.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val myStruct = p.getDeclarationsByName("p.MyStruct", RecordDeclaration::class.java).iterator().next()

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

        val myInterface = p.getDeclarationsByName("p.MyInterface", RecordDeclaration::class.java).iterator().next()

        assertNotNull(myInterface)
        assertEquals("interface", myInterface.kind)

        methods = myInterface.methods

        assertEquals(1, methods.size)

        myFunc = methods.first()

        assertEquals("MyFunc", myFunc.name)
        assertEquals(TypeParser.createFrom("string", false), myFunc.type)
    }

    @Test
    fun testMemberCalls() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("struct.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val myStruct = p.getDeclarationsByName("p.MyStruct", RecordDeclaration::class.java).iterator().next()

        val methods = myStruct.methods

        val myFunc = methods.first()

        assertEquals("MyFunc", myFunc.name)

        val body = myFunc.body as? CompoundStatement

        assertNotNull(body)

        val printf = body.statements.first() as? CallExpression

        assertNotNull(printf)
        assertEquals("Printf", printf.name)
        assertEquals("fmt.Printf", printf.fqn)

        val arg1 = printf.arguments[0] as? MemberCallExpression

        assertNotNull(arg1)
        assertEquals("myOtherFunc", arg1.name)
        assertEquals("s.myOtherFunc", arg1.fqn)

        assertEquals(myFunc.receiver, (arg1.base as? DeclaredReferenceExpression)?.refersTo)
    }

    @Test
    fun testField() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("field.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val myFunc = p.getDeclarationsByName("myFunc", MethodDeclaration::class.java).iterator().next()

        val body = myFunc.body as? CompoundStatement

        assertNotNull(body)

        val binOp = body.statements.first() as? BinaryOperator

        assertNotNull(binOp)

        val lhs = binOp.lhs as? MemberExpression

        assertNotNull(lhs)
        assertEquals(myFunc.receiver, (lhs.base as? DeclaredReferenceExpression)?.refersTo)
        assertEquals("Field", lhs.name)
        assertEquals(TypeParser.createFrom("int", false), lhs.type)

        val rhs = binOp.rhs as? MemberExpression

        assertNotNull(rhs)
        assertEquals("otherPackage", (rhs.base as? DeclaredReferenceExpression)?.name)
        assertEquals("OtherField", rhs.name)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("if.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val main = p.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)

        val body = main.body as? CompoundStatement

        assertNotNull(body)

        val b = (body.statements.first() as? DeclarationStatement)?.singleDeclaration as? VariableDeclaration

        assertNotNull(b)
        assertEquals("b", b.name)
        assertEquals(TypeParser.createFrom("bool", false), b.type)

        // true, false are builtin variables, NOT literals in Golang
        // we might need to parse this special case differently
        val initializer = b.initializer as? DeclaredReferenceExpression

        assertNotNull(initializer)
        assertEquals("true", initializer.name)

        val `if` = body.statements[1] as? IfStatement

        assertNotNull(`if`)
    }

    @Test
    fun testSwitch() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("switch.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val myFunc = p.getDeclarationsByName("myFunc", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(myFunc)

        val body = myFunc.body as? CompoundStatement

        assertNotNull(body)

        val switch = body.statements.first() as? SwitchStatement

        assertNotNull(switch)
        
        val list = switch.statement as? CompoundStatement

        assertNotNull(list)

        val case1 = list.statements[0] as? CaseStatement

        assertNotNull(case1)
        assertEquals(1, (case1.caseExpression as? Literal<*>)?.value)

        val first = list.statements[1] as? CallExpression

        assertNotNull(first)
        assertEquals("first", first.name)

        val case2 = list.statements[2] as? CaseStatement

        assertNotNull(case2)
        assertEquals(2, (case2.caseExpression as? Literal<*>)?.value)

        val second = list.statements[3] as? CallExpression

        assertNotNull(second)
        assertEquals("second", second.name)

        val case3 = list.statements[4] as? CaseStatement

        assertNotNull(case3)
        assertEquals(3, (case3.caseExpression as? Literal<*>)?.value)

        val third = list.statements[5] as? CallExpression

        assertNotNull(third)
        assertEquals("third", third.name)
    }
}