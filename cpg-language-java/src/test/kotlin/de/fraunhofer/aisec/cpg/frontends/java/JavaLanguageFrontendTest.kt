/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.java

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.TestUtils.analyzeWithBuilder
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TranslationManager.Companion.builder
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.test.*

internal class JavaLanguageFrontendTest : BaseTest() {
    @Test
    fun testLargeNegativeNumber() {
        val file = File("src/test/resources/LargeNegativeNumber.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }

        val declaration = tu.byNameOrNull<RecordDeclaration>("LargeNegativeNumber")
        assertNotNull(declaration)

        val main = declaration.byNameOrNull<MethodDeclaration>("main")
        assertNotNull(main)

        val a = main.variables["a"]
        assertNotNull(a)
        assertEquals(1, ((a.initializer as? UnaryOperator)?.input as? Literal<*>)?.value)

        val b = main.variables["b"]
        assertNotNull(b)
        assertEquals(2147483648L, ((b.initializer as? UnaryOperator)?.input as? Literal<*>)?.value)

        val c = main.variables["c"]
        assertNotNull(c)
        assertEquals(
            BigInteger("9223372036854775808"),
            ((c.initializer as? UnaryOperator)?.input as? Literal<*>)?.value
        )

        val d = main.variables["d"]
        assertNotNull(d)
        assertEquals(
            9223372036854775807L,
            ((d.initializer as? UnaryOperator)?.input as? Literal<*>)?.value
        )
    }

    @Test
    fun testFor() {
        val file = File("src/test/resources/components/ForStmt.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }

        val declaration = tu.declarations[0] as? RecordDeclaration
        assertNotNull(declaration)

        val main = declaration.methods[0]
        val ls = main.variables["ls"]
        assertNotNull(ls)

        val forStatement = main.getBodyStatementAs(2, ForStatement::class.java)
        assertNotNull(forStatement)

        // initializer is an expression list
        val list = forStatement.initializerStatement as? ExpressionList
        assertNotNull(list)

        // check calculated location of sub-expression
        val location = list.location
        assertNotNull(location)
        assertEquals(Region(9, 10, 9, 22), location.region)
    }

    @Test
    fun testForeach() {
        val file = File("src/test/resources/components/ForEachStmt.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        val declaration = tu.declarations[0] as? RecordDeclaration
        assertNotNull(declaration)

        val main = declaration.methods[0]
        val ls = main.variables["ls"]
        assertNotNull(ls)

        val forEachStatement = main.getBodyStatementAs(1, ForEachStatement::class.java)
        assertNotNull(forEachStatement)

        // should loop over ls
        assertEquals(ls, (forEachStatement.iterable as? DeclaredReferenceExpression)?.refersTo)

        // should declare String s
        val s = forEachStatement.variable
        assertNotNull(s)
        assertTrue(s is DeclarationStatement)
        assertTrue(s.isSingleDeclaration())

        val sDecl = s.singleDeclaration as? VariableDeclaration
        assertNotNull(sDecl)
        assertLocalName("s", sDecl)
        assertEquals(createTypeFrom("java.lang.String"), sDecl.type)

        // should contain a single statement
        val sce = forEachStatement.statement as? MemberCallExpression
        assertNotNull(sce)

        assertLocalName("println", sce)
        assertFullName("java.io.PrintStream.println", sce)

        // Check the flow from the iterable to the variable s
        assertEquals(1, sDecl.prevDFG.size)
        assertTrue(forEachStatement.iterable as DeclaredReferenceExpression in sDecl.prevDFG)
        // Check the flow from the variable s to the print
        assertTrue(sDecl in sce.arguments.first().prevDFG)
    }

    @Test
    fun testTryCatch() {
        val file = File("src/test/resources/components/TryStmt.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }

        val declaration = tu.declarations[0] as? RecordDeclaration
        assertNotNull(declaration)

        val main = declaration.methods[0]

        // lets get our try statement
        val tryStatement = main.bodyOrNull<TryStatement>(0)
        assertNotNull(tryStatement)

        var scope = tryStatement.scope
        assertNotNull(scope)

        // should have 3 catch clauses
        val catchClauses = tryStatement.catchClauses
        assertEquals(3, catchClauses.size)

        val firstCatch = catchClauses.firstOrNull()
        assertNotNull(firstCatch)

        scope = firstCatch.scope
        assertNotNull(scope)

        // first exception type was? resolved, so we can expect a FQN
        assertEquals(createTypeFrom("java.lang.NumberFormatException"), firstCatch.parameter?.type)
        // second one could not be resolved so we do not have an FQN
        assertEquals(createTypeFrom("NotResolvableTypeException"), catchClauses[1].parameter?.type)
        // third type should have been resolved through the import
        assertEquals(createTypeFrom("some.ImportedException"), (catchClauses[2].parameter)?.type)

        // and 1 finally
        val finallyBlock = tryStatement.finallyBlock
        assertNotNull(finallyBlock)

        scope = finallyBlock.scope
        assertNotNull(scope)
    }

    @Test
    fun testLiteral() {
        val file = File("src/test/resources/components/LiteralExpr.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }

        val declaration = tu.declarations[0] as? RecordDeclaration
        assertNotNull(declaration)

        val main = declaration.methods[0]

        // int i = 1;
        val i =
            (main.getBodyStatementAs(0, DeclarationStatement::class.java))?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(i)
        var literal = i.initializer as? Literal<*>
        assertNotNull(literal)
        assertEquals(1, literal.value)

        // String s = "string";
        val s =
            (main.getBodyStatementAs(1, DeclarationStatement::class.java))?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(s)
        literal = s.initializer as? Literal<*>
        assertNotNull(literal)
        assertEquals("string", literal.value)

        // boolean b = true;
        val b =
            (main.getBodyStatementAs(2, DeclarationStatement::class.java))?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(b)
        literal = b.initializer as? Literal<*>
        assertNotNull(literal)
        assertEquals(true, literal.value)

        // char c = '0';
        val c =
            (main.getBodyStatementAs(3, DeclarationStatement::class.java))?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(c)
        literal = c.initializer as? Literal<*>
        assertNotNull(literal)
        assertEquals('0', literal.value)

        // double d = 1.0;
        val d =
            (main.getBodyStatementAs(4, DeclarationStatement::class.java))?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(d)
        literal = d.initializer as? Literal<*>
        assertNotNull(literal)
        assertEquals(1.0, literal.value)

        // long l = 1L;
        val l =
            (main.getBodyStatementAs(5, DeclarationStatement::class.java))?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(l)
        literal = l.initializer as? Literal<*>
        assertNotNull(literal)
        assertEquals(1L, literal.value)

        // Object o = null;
        val o =
            (main.getBodyStatementAs(6, DeclarationStatement::class.java))?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(o)
        literal = o.initializer as? Literal<*>
        assertNotNull(literal)
        assertNull(literal.value)
    }

    @Test
    fun testRecordDeclaration() {
        val file = File("src/test/resources/compiling/RecordDeclaration.java")
        val declaration =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        // TODO: Use GraphExamples here as well.
        assertNotNull(declaration)

        val namespaceDeclaration = declaration.getDeclarationAs(0, NamespaceDeclaration::class.java)

        val recordDeclaration =
            namespaceDeclaration?.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(recordDeclaration)

        val fields = recordDeclaration.fields.map { it.name.localName }
        assertTrue(fields.contains("field"))

        val method = recordDeclaration.methods[0]
        assertNotNull(method)
        assertEquals(recordDeclaration, method.recordDeclaration)
        assertLocalName("method", method)
        assertEquals(createTypeFrom("java.lang.Integer"), method.returnTypes.firstOrNull())

        val functionType = method.type as? FunctionType
        assertNotNull(functionType)
        assertLocalName("method()java.lang.Integer", functionType)

        val constructor = recordDeclaration.constructors[0]
        assertEquals(recordDeclaration, constructor.recordDeclaration)
        assertLocalName("SimpleClass", constructor)
    }

    @Test
    fun testNameExpressions() {
        val file = File("src/test/resources/compiling/NameExpression.java")
        val declaration =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        assertNotNull(declaration)
    }

    @Test
    fun testSwitch() {
        val file = File("src/test/resources/cfg/Switch.java")
        val declaration =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        val graphNodes = SubgraphWalker.flattenAST(declaration)

        assertTrue(graphNodes.size != 0)

        val switchStatements = graphNodes.filterIsInstance<SwitchStatement>()
        assertEquals(3, switchStatements.size)

        val switchStatement = switchStatements[0]
        assertEquals(11, (switchStatement.statement as? CompoundStatement)?.statements?.size)

        val caseStatements = switchStatement.allChildren<CaseStatement>()
        assertEquals(4, caseStatements.size)

        val defaultStatements = switchStatement.allChildren<DefaultStatement>()
        assertEquals(1, defaultStatements.size)
    }

    @Test
    fun testCast() {
        val file = File("src/test/resources/components/CastExpr.java")
        val declaration =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        assertNotNull(declaration)

        val namespaceDeclaration = declaration.getDeclarationAs(0, NamespaceDeclaration::class.java)
        assertNotNull(namespaceDeclaration)

        val record = namespaceDeclaration.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(record)
        val main = record.methods[0]
        assertNotNull(main)

        // e = new ExtendedClass()
        var stmt = main.getBodyStatementAs(0, DeclarationStatement::class.java)
        assertNotNull(stmt)

        val e = stmt.getSingleDeclarationAs(VariableDeclaration::class.java)
        // This test can be simplified once we solved the issue with inconsistently used simple
        // names
        // vs. fully qualified names.
        assertTrue(
            e.type?.name?.localName == "ExtendedClass" ||
                e.type?.name?.toString() == "cast.ExtendedClass"
        )

        // b = (BaseClass) e
        stmt = main.getBodyStatementAs(1, DeclarationStatement::class.java)
        assertNotNull(stmt)

        val b = stmt.getSingleDeclarationAs(VariableDeclaration::class.java)
        assertTrue(
            b.type?.name?.localName == "BaseClass" || b.type?.name?.toString() == "cast.BaseClass"
        )

        // initializer
        val cast = b.initializer as? CastExpression
        assertNotNull(cast)
        assertTrue(
            cast.type.name.localName == "BaseClass" ||
                cast.type.name?.toString() == "cast.BaseClass"
        )

        // expression itself should be a reference
        val ref = cast.expression as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertEquals(e, ref.refersTo)
    }

    @Test
    fun testArrays() {
        val file = File("src/test/resources/compiling/Arrays.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        assertNotNull(tu)

        val namespaceDeclaration = tu.getDeclarationAs(0, NamespaceDeclaration::class.java)
        assertNotNull(namespaceDeclaration)

        val record = namespaceDeclaration.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(record)

        val main = record.methods[0]
        assertNotNull(main)

        val statements = (main.body as? CompoundStatement)?.statements
        assertNotNull(statements)

        val a = (statements[0] as? DeclarationStatement)?.singleDeclaration as? VariableDeclaration
        assertNotNull(a)

        // type should be Integer[]
        assertEquals(createTypeFrom("int[]"), a.type)

        // it has an array creation initializer
        val ace = a.initializer as? ArrayCreationExpression
        assertNotNull(ace)

        // which has a initializer list (1 entry)
        val ile = ace.initializer as? InitializerListExpression
        assertNotNull(ile)
        assertEquals(1, ile.initializers.size)

        // first one is an int literal
        val literal = ile.initializers[0] as? Literal<*>
        assertEquals(1, literal?.value)

        // next one is a declaration with array subscription
        val b = (statements[1] as? DeclarationStatement)?.singleDeclaration as? VariableDeclaration

        // initializer is array subscription
        val ase = b?.initializer as? ArraySubscriptionExpression
        assertNotNull(ase)
        assertEquals(a, (ase.arrayExpression as? DeclaredReferenceExpression)?.refersTo)
        assertEquals(0, (ase.subscriptExpression as? Literal<*>)?.value)
    }

    @Test
    fun testFieldAccessExpressions() {
        val file = File("src/test/resources/compiling/FieldAccess.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        assertNotNull(tu)

        val namespaceDeclaration = tu.getDeclarationAs(0, NamespaceDeclaration::class.java)
        assertNotNull(namespaceDeclaration)

        val record = namespaceDeclaration.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(record)

        val main = record.methods[0]
        assertNotNull(main)

        val statements = (main.body as? CompoundStatement)?.statements
        assertNotNull(statements)

        val l = (statements[1] as? DeclarationStatement)?.singleDeclaration as? VariableDeclaration
        assertLocalName("l", l)

        val length = l?.initializer as? MemberExpression
        assertNotNull(length)
        assertLocalName("length", length)
        assertLocalName("int", length.type)
    }

    @Test
    fun testMemberCallExpressions() {
        val file = File("src/test/resources/compiling/MemberCallExpression.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        assertNotNull(tu)

        assertEquals(7, tu.mcalls.size)
        assertTrue(tu.mcalls.all { !it.isStatic })
    }

    @Test
    fun testLocation() {
        val file = File("src/test/resources/compiling/FieldAccess.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        assertNotNull(tu)

        val namespaceDeclaration = tu.getDeclarationAs(0, NamespaceDeclaration::class.java)
        assertNotNull(namespaceDeclaration)

        val record = namespaceDeclaration.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(record)

        val main = record.methods[0]
        assertNotNull(main)

        val location = main.location
        assertNotNull(location)

        val path = Path.of(location.artifactLocation.uri)
        assertEquals("FieldAccess.java", path.fileName.toString())
        assertEquals(Region(7, 3, 10, 4), location.region)
    }

    @Test
    fun testAnnotations() {
        val file = File("src/test/resources/Annotation.java")
        val declarations =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .defaultPasses()
                    .defaultLanguages()
                    .registerLanguage(JavaLanguage())
                    .processAnnotations(true)
            )
        assertFalse(declarations.isEmpty())

        val tu = declarations[0]
        assertNotNull(tu)

        val record = tu.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(record)

        var annotations: List<Annotation> = record.annotations
        assertEquals(1, annotations.size)

        val forClass = annotations[0]
        assertLocalName("AnnotationForClass", forClass)

        var value = forClass.members[0]
        assertLocalName("value", value)
        assertEquals(2, (value.value as? Literal<*>)?.value)

        var field = record.fields["field"]
        assertNotNull(field)
        annotations = field.annotations
        assertEquals(1, annotations.size)

        var forField = annotations[0]
        assertLocalName("AnnotatedField", forField)

        field = record.fields["anotherField"]
        assertNotNull(field)

        annotations = field.annotations
        assertEquals(1, annotations.size)

        forField = annotations[0]
        assertLocalName("AnnotatedField", forField)

        value = forField.members[0]
        assertLocalName(JavaLanguageFrontend.ANNOTATION_MEMBER_VALUE, value)
        assertEquals("myString", (value.value as? Literal<*>)?.value)
    }

    @Test
    fun testChainedCalls() {
        val file = File("src/test/resources/Issue285.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        val record = tu.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(record)

        val func = record.methods.stream().findFirst().orElse(null)
        assertNotNull(func)
        assertNotNull(func.receiver)

        val nodes = SubgraphWalker.flattenAST(record)
        val request =
            nodes
                .stream()
                .filter { node: Node ->
                    (node is VariableDeclaration && "request" == node.name.localName)
                }
                .map { node: Node? -> node as? VariableDeclaration? }
                .findFirst()
                .orElse(null)
        assertNotNull(request)

        val initializer = request.initializer
        assertNotNull(initializer)
        assertTrue(initializer is MemberCallExpression)

        val call = initializer as? MemberCallExpression
        assertLocalName("get", call)
        val staticCall = nodes.filterIsInstance<MemberCallExpression>().firstOrNull { it.isStatic }
        assertNotNull(staticCall)
        assertLocalName("doSomethingStatic", staticCall)
    }

    @Test
    fun testSuperFieldUsage() {
        val file1 = File("src/test/resources/fix-328/Cat.java")
        val file2 = File("src/test/resources/fix-328/Animal.java")
        val result =
            TestUtils.analyze(listOf(file1, file2), file1.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        val tu = findByUniqueName(result.translationUnits, "src/test/resources/fix-328/Cat.java")
        val namespace = tu.getDeclarationAs(0, NamespaceDeclaration::class.java)
        assertNotNull(namespace)

        val record = namespace.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(record)

        val constructor = record.constructors[0]
        val op = constructor.getBodyStatementAs(0, BinaryOperator::class.java)
        assertNotNull(op)

        val lhs = op.lhs as? MemberExpression
        val receiver =
            (lhs?.base as? DeclaredReferenceExpression)?.refersTo as? VariableDeclaration?
        assertNotNull(receiver)
        assertLocalName("this", receiver)
        assertEquals(createTypeFrom("my.Animal"), receiver.type)
    }

    @Test
    fun testOverrideHandler() {
        /** A simple extension of the [JavaLanguageFrontend] to demonstrate handler overriding. */
        class MyJavaLanguageFrontend(
            language: JavaLanguage,
            config: TranslationConfiguration,
            scopeManager: ScopeManager,
        ) : JavaLanguageFrontend(language, config, scopeManager) {
            init {
                this.declarationHandler =
                    object : DeclarationHandler(this@MyJavaLanguageFrontend) {
                        override fun handleClassOrInterfaceDeclaration(
                            classInterDecl: ClassOrInterfaceDeclaration
                        ): RecordDeclaration {
                            // take the original class and replace the name
                            val declaration =
                                super.handleClassOrInterfaceDeclaration(classInterDecl)
                            declaration.name =
                                Name(
                                    "MySimpleClass",
                                    declaration.name.parent,
                                    declaration.name.delimiter
                                )

                            return declaration
                        }
                    }
            }
        }

        class MyJavaLanguage : JavaLanguage() {
            override val fileExtensions = listOf("java")
            override val namespaceDelimiter = "."
            override val superClassKeyword = "super"
            override val frontend = MyJavaLanguageFrontend::class
            override fun newFrontend(
                config: TranslationConfiguration,
                scopeManager: ScopeManager,
            ): MyJavaLanguageFrontend {
                return MyJavaLanguageFrontend(this, config, scopeManager)
            }
        }

        val file = File("src/test/resources/compiling/RecordDeclaration.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(JavaLanguage::class.java)
                it.registerLanguage(MyJavaLanguage())
            }

        assertNotNull(tu)

        val compiling = tu.byNameOrNull<NamespaceDeclaration>("compiling")
        assertNotNull(compiling)

        val recordDeclaration = compiling.byNameOrNull<RecordDeclaration>("MySimpleClass")
        assertNotNull(recordDeclaration)
    }

    @Test
    @Throws(Exception::class)
    fun testHierarchy() {
        val topLevel = Paths.get("src/test/resources/compiling/hierarchy")
        val files =
            Files.walk(topLevel, Int.MAX_VALUE)
                .map { obj: Path -> obj.toFile() }
                .filter { obj: File -> obj.isFile }
                .filter { f: File -> f.name.endsWith(".java") }
                .collect(Collectors.toList())
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(*files.toTypedArray())
                .topLevel(topLevel.toFile())
                .defaultPasses()
                .defaultLanguages()
                .registerLanguage(JavaLanguage())
                .debugParser(true)
                .failOnError(true)
                .build()
        val analyzer = builder().config(config).build()
        val result = analyzer.analyze().get()
        assertNotNull(result)
    }

    @Test
    @Throws(Exception::class)
    fun testPartial() {
        val topLevel = Paths.get("src/test/resources/partial")
        val files =
            Files.walk(topLevel, Int.MAX_VALUE)
                .map { obj: Path -> obj.toFile() }
                .filter { obj: File -> obj.isFile }
                .filter { f: File -> f.name.endsWith(".java") }
                .collect(Collectors.toList())
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(*files.toTypedArray())
                .topLevel(topLevel.toFile())
                .defaultPasses()
                .defaultLanguages()
                .registerLanguage(JavaLanguage())
                .debugParser(true)
                .failOnError(true)
                .build()
        val analyzer = builder().config(config).build()
        val result = analyzer.analyze().get()
        for (node in result.translationUnits) {
            assertNotNull(node)
        }
    }

    @Test
    fun testQualifiedThis() {
        val file = File("src/test/resources/compiling/OuterClass.java")
        val result =
            TestUtils.analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }
        val tu = result.translationUnits.firstOrNull()
        assertNotNull(tu)

        val outerClass = tu.records["compiling.OuterClass"]
        assertNotNull(outerClass)

        val innerClass = outerClass.records["InnerClass"]
        assertNotNull(innerClass)

        val thisOuterClass = innerClass.fields["this\$OuterClass"]
        assertNotNull(thisOuterClass)

        val evenMoreInnerClass = innerClass.records["EvenMoreInnerClass"]
        assertNotNull(evenMoreInnerClass)

        val thisInnerClass = evenMoreInnerClass.fields["this\$InnerClass"]
        assertNotNull(thisInnerClass)

        val doSomething = evenMoreInnerClass.methods["doSomething"]
        assertNotNull(doSomething)

        val binOp = doSomething.bodyOrNull<BinaryOperator>()
        assertNotNull(binOp)

        val ref = ((binOp.rhs as? MemberExpression)?.base as DeclaredReferenceExpression).refersTo
        assertNotNull(ref)
        assertSame(ref, thisOuterClass)
    }

    private fun createTypeFrom(typename: String) = TypeParser.createFrom(typename, JavaLanguage())

    @Test
    fun testForEach() {
        val file = File("src/test/resources/compiling/ForEach.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage(JavaLanguage())
            }

        val p = tu.namespaces["compiling"]
        val forEachClass = p.records["compiling.ForEach"]
        val forIterator = forEachClass.methods["forIterator"]
        assertNotNull(forIterator)

        val forEach = forIterator.bodyOrNull<ForEachStatement>()
        assertNotNull(forEach)

        val loopVariable = (forEach.variable as? DeclarationStatement)?.singleDeclaration
        assertNotNull(loopVariable)
        assertNotNull(forEach.iterable)
        assertContains(loopVariable.prevDFG, forEach.iterable!!)

        val jArg = forIterator.calls["println"]?.arguments?.firstOrNull()
        assertNotNull(jArg)
        assertContains(jArg.prevDFG, loopVariable)
    }
}
