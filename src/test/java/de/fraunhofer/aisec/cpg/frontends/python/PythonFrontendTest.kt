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
    
    private var config: TranslationConfiguration? = null
    @BeforeEach
    fun setUp() {
        config = TranslationConfiguration.builder().build()
    }

    val topLevel = Path.of("src", "test", "resources", "python")


    @Test
    @Throws(TranslationException::class)
    fun testMax() {
        // TODO: what's happening here?
        //val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("simple.py").toFile()), topLevel, true)
        //assertNotNull(tu)
    }

    @Test
    @Throws(TranslationException::class)
    fun testPythonImpl() {
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(File("python","main.py")), topLevel, true)
        assertNotNull(tu)
    }

    @Test
    @Throws(TranslationException::class)
    fun testEmpty() { // parse an empty file
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("test_empty.py").toFile()), topLevel, true)
        assertNotNull(tu)
        assert(tu.declarations.isNotEmpty())
        assert(tu.declarations.size == 1)
        assert(tu.name == "src/test/resources/python/test_empty.py")
        val nsd = tu.declarations.get(0) as? NamespaceDeclaration
    }

    @Test
    @Throws(TranslationException::class)
    fun testSimple01() { // parse a trivial file
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("test_simple_func_01.py").toFile()), topLevel, true)
        assertNotNull(tu)
        assert(tu.declarations.isNotEmpty())
        assert(tu.declarations.size == 1)
        val decl = tu.declarations.get(0) as? FunctionDeclaration
        assert(decl?.parameters?.size == 0)
        assert(decl?.name.equals("funcname"))
        val body = decl?.body as? CompoundStatement
        assert(body?.statements?.size == 1)
        // TODO? val stmt = body?.statements?.get(0)?.end as? EmptyStatement
    }

    @Test
    @Throws(TranslationException::class)
    fun testSimple() {
        val declaration = config?.let {
            PythonLanguageFrontend(it, ScopeManager())
                .parse(File(topLevel.resolve("simple.py").toString()))
        }
        assertNotNull(declaration)
        val declarations = declaration.declarations
        assertEquals(1, declarations.size)

        // first declaration is the function declaration
        assertTrue(declarations[0] is FunctionDeclaration)

        val functionDeclaration = declarations[0] as FunctionDeclaration
        assertEquals("test", functionDeclaration.name)

        val body = functionDeclaration.body as CompoundStatement
        val statements = body.statements

        assertEquals(5, statements.size)
        val stmt = statements[1]
        assertTrue(stmt is CallExpression)

        val call = stmt as CallExpression
        assertEquals("print", call.name)

        // k = 3
        val binOp = statements[0] as? BinaryOperator

        assertNotNull(binOp)

        val lhs = binOp.lhs as? Expression
        val target = lhs as? DeclaredReferenceExpression

        assertNotNull(target)

        assertEquals("k", target?.name)
    }

    /*
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

    @Test
    fun testMemberCalls() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(topLevel.resolve("struct.go").toFile()), topLevel, true)

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val myStruct = p.getDeclarationsByName("MyStruct", RecordDeclaration::class.java).iterator().next()

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
    */
}
