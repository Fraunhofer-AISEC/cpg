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

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.TestUtils.analyzeWithBuilder
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.helpers.NodeComparator
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.math.BigInteger
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import kotlin.test.*
import org.junit.jupiter.api.Test

internal class JavaLanguageFrontendTest : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun testLargeNegativeNumber() {
        val file = File("src/test/resources/LargeNegativeNumber.java")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        val declaration = tu.byNameOrNull<RecordDeclaration>("LargeNegativeNumber")
        assertNotNull(declaration)

        val main = declaration.byNameOrNull<MethodDeclaration>("main")
        assertNotNull(main)

        val a = main.getVariableDeclarationByName("a").orElse(null)
        assertNotNull(a)
        assertEquals(
            1,
            (Objects.requireNonNull(a.getInitializerAs(UnaryOperator::class.java))?.input as?
                    Literal<*>)
                ?.value
        )
        val b = main.getVariableDeclarationByName("b").orElse(null)
        assertNotNull(b)
        assertEquals(
            2147483648L,
            (Objects.requireNonNull(b.getInitializerAs(UnaryOperator::class.java))?.input as?
                    Literal<*>)
                ?.value
        )
        val c = main.getVariableDeclarationByName("c").orElse(null)
        assertNotNull(c)
        assertEquals(
            BigInteger("9223372036854775808"),
            (Objects.requireNonNull(c.getInitializerAs(UnaryOperator::class.java))?.input as?
                    Literal<*>)
                ?.value
        )
        val d = main.getVariableDeclarationByName("d").orElse(null)
        assertNotNull(d)
        assertEquals(
            9223372036854775807L,
            (Objects.requireNonNull(d.getInitializerAs(UnaryOperator::class.java))?.input as?
                    Literal<*>)
                ?.value
        )
    }

    @Test
    @Throws(Exception::class)
    fun testFor() {
        val file = File("src/test/resources/components/ForStmt.java")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        val declaration = tu.declarations[0] as? RecordDeclaration
        assertNotNull(declaration)

        val main = declaration.methods[0]
        val ls = main.getVariableDeclarationByName("ls").orElse(null)
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
    @Throws(Exception::class)
    fun testForeach() {
        val file = File("src/test/resources/components/ForEachStmt.java")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val declaration = tu.declarations[0] as? RecordDeclaration
        assertNotNull(declaration)

        val main = declaration.methods[0]
        val ls = main.getVariableDeclarationByName("ls").orElse(null)
        assertNotNull(ls)

        val forEachStatement = main.getBodyStatementAs(1, ForEachStatement::class.java)
        assertNotNull(forEachStatement)

        // should loop over ls
        assertEquals(ls, (forEachStatement.iterable as? DeclaredReferenceExpression)?.refersTo)

        // should declare String s
        val s = forEachStatement.variable
        assertNotNull(s)
        assertTrue(s is DeclarationStatement)
        assertTrue(s.isSingleDeclaration)

        val sDecl = s.singleDeclaration as? VariableDeclaration
        assertNotNull(sDecl)
        assertEquals("s", sDecl.name)
        assertEquals(TypeParser.createFrom("java.lang.String", true), sDecl.type)

        // should contain a single statement
        val sce = forEachStatement.statement as? MemberCallExpression
        assertNotNull(sce)
        assertEquals("println", sce.name)
        assertEquals("java.io.PrintStream.println", sce.fqn)
    }

    @Test
    @Throws(Exception::class)
    fun testTryCatch() {
        val file = File("src/test/resources/components/TryStmt.java")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        val declaration = tu.declarations[0] as? RecordDeclaration
        assertNotNull(declaration)

        val main = declaration.methods[0]

        // lets get our try statement
        val tryStatement = main.bodyOrNull<TryStatement>(0)
        assertNotNull(tryStatement)

        // should have 3 catch clauses
        val catchClauses = tryStatement.catchClauses
        assertEquals(3, catchClauses.size)
        // first exception type was? resolved, so we can expect a FQN
        assertEquals(
            TypeParser.createFrom("java.lang.NumberFormatException", true),
            Objects.requireNonNull(catchClauses[0].parameter)?.type
        )
        // second one could not be resolved so we do not have an FQN
        assertEquals(
            TypeParser.createFrom("NotResolvableTypeException", true),
            Objects.requireNonNull(catchClauses[1].parameter)?.type
        )
        // third type should have been resolved through the import
        assertEquals(
            TypeParser.createFrom("some.ImportedException", true),
            Objects.requireNonNull(catchClauses[2].parameter)?.type
        )

        // and 1 finally
        val finallyBlock = tryStatement.finallyBlock
        assertNotNull(finallyBlock)
    }

    @Test
    @Throws(Exception::class)
    fun testLiteral() {
        val file = File("src/test/resources/components/LiteralExpr.java")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        val declaration = tu.declarations[0] as? RecordDeclaration
        assertNotNull(declaration)

        val main = declaration.methods[0]

        // int i = 1;
        val i =
            Objects.requireNonNull(main.getBodyStatementAs(0, DeclarationStatement::class.java))
                ?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(i)
        var literal = i.getInitializerAs(Literal::class.java)
        assertNotNull(literal)
        assertEquals(1, literal.value)

        // String s = "string";
        val s =
            Objects.requireNonNull(main.getBodyStatementAs(1, DeclarationStatement::class.java))
                ?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(s)
        literal = s.getInitializerAs(Literal::class.java)
        assertNotNull(literal)
        assertEquals("string", literal.value)

        // boolean b = true;
        val b =
            Objects.requireNonNull(main.getBodyStatementAs(2, DeclarationStatement::class.java))
                ?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(b)
        literal = b.getInitializerAs(Literal::class.java)
        assertNotNull(literal)
        assertEquals(true, literal.value)

        // char c = '0';
        val c =
            Objects.requireNonNull(main.getBodyStatementAs(3, DeclarationStatement::class.java))
                ?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(c)
        literal = c.getInitializerAs(Literal::class.java)
        assertNotNull(literal)
        assertEquals('0', literal.value)

        // double d = 1.0;
        val d =
            Objects.requireNonNull(main.getBodyStatementAs(4, DeclarationStatement::class.java))
                ?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(d)
        literal = d.getInitializerAs(Literal::class.java)
        assertNotNull(literal)
        assertEquals(1.0, literal.value)

        // long l = 1L;
        val l =
            Objects.requireNonNull(main.getBodyStatementAs(5, DeclarationStatement::class.java))
                ?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(l)
        literal = l.getInitializerAs(Literal::class.java)
        assertNotNull(literal)
        assertEquals(1L, literal.value)

        // Object o = null;
        val o =
            Objects.requireNonNull(main.getBodyStatementAs(6, DeclarationStatement::class.java))
                ?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(o)
        literal = o.getInitializerAs(Literal::class.java)
        assertNotNull(literal)
        assertNull(literal.value)
    }

    @Test
    @Throws(Exception::class)
    fun testRecordDeclaration() {
        val file = File("src/test/resources/compiling/RecordDeclaration.java")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        assertNotNull(declaration)
        val namespaceDeclaration = declaration.getDeclarationAs(0, NamespaceDeclaration::class.java)

        val recordDeclaration =
            namespaceDeclaration?.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(recordDeclaration)

        val fields =
            recordDeclaration
                .fields
                .stream()
                .map(FieldDeclaration::name)
                .collect(Collectors.toList())
        assertTrue(fields.contains("this"))
        assertTrue(fields.contains("field"))
        val method = recordDeclaration.methods[0]
        assertEquals(recordDeclaration, method.recordDeclaration)
        assertEquals("method", method.name)
        assertEquals(TypeParser.createFrom("java.lang.Integer", true), method?.type)
        val constructor = recordDeclaration.constructors[0]
        assertEquals(recordDeclaration, constructor.recordDeclaration)
        assertEquals("SimpleClass", constructor.name)
    }

    @Test
    @Throws(Exception::class)
    fun testNameExpressions() {
        val file = File("src/test/resources/compiling/NameExpression.java")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        assertNotNull(declaration)
    }

    @Test
    @Throws(Exception::class)
    fun testSwitch() {
        val file = File("src/test/resources/cfg/Switch.java")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val graphNodes = SubgraphWalker.flattenAST(declaration)
        graphNodes.sortWith(NodeComparator())
        assertTrue(graphNodes.size != 0)

        val switchStatements = Util.filterCast(graphNodes, SwitchStatement::class.java)
        assertEquals(3, switchStatements.size)

        val switchStatement = switchStatements[0]
        assertEquals(11, (switchStatement.statement as? CompoundStatement)?.statements?.size)

        val caseStatements =
            Util.filterCast(SubgraphWalker.flattenAST(switchStatement), CaseStatement::class.java)
        assertEquals(4, caseStatements.size)

        val defaultStatements =
            Util.filterCast(
                SubgraphWalker.flattenAST(switchStatement),
                DefaultStatement::class.java
            )
        assertEquals(1, defaultStatements.size)
    }

    @Test
    @Throws(Exception::class)
    fun testCast() {
        val file = File("src/test/resources/components/CastExpr.java")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
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
        assertTrue(e?.type?.name == "ExtendedClass" || e?.type?.name == "cast.ExtendedClass")

        // b = (BaseClass) e
        stmt = main.getBodyStatementAs(1, DeclarationStatement::class.java)
        assertNotNull(stmt)
        val b = stmt.getSingleDeclarationAs(VariableDeclaration::class.java)
        assertTrue(b?.type?.name == "BaseClass" || b?.type?.name == "cast.BaseClass")

        // initializer
        val cast = b.initializer as? CastExpression
        assertNotNull(cast)
        assertTrue(cast.type.name == "BaseClass" || cast.type.name == "cast.BaseClass")

        // expression itself should be a reference
        val ref = cast.expression as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertEquals(e, ref.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testArrays() {
        val file = File("src/test/resources/compiling/Arrays.java")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
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
        assertEquals(TypeParser.createFrom("int[]", true), a.type)

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
    @Throws(Exception::class)
    fun testFieldAccessExpressions() {
        val file = File("src/test/resources/compiling/FieldAccess.java")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
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
        assertEquals("l", l?.name)

        val length = l?.initializer as? MemberExpression
        assertNotNull(length)
        assertEquals("length", length.name)
        assertEquals("int", length.type?.typeName)
    }

    @Test
    @Throws(Exception::class)
    fun testMemberCallExpressions() {
        val file = File("src/test/resources/compiling/MemberCallExpression.java")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        assertNotNull(tu)

        val count = SubgraphWalker.flattenAST(tu).count { it is MemberCallExpression }

        assertEquals(6, count)
    }

    @Test
    @Throws(Exception::class)
    fun testLocation() {
        val file = File("src/test/resources/compiling/FieldAccess.java")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
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
    @Throws(Exception::class)
    fun testAnnotations() {
        val file = File("src/test/resources/Annotation.java")
        val declarations =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .defaultPasses()
                    .defaultLanguages()
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
        assertEquals("AnnotationForClass", forClass.name)

        var value = forClass.members[0]
        assertEquals("value", value.name)
        assertEquals(2, (value.value as? Literal<*>)?.value)

        var field = record.getField("field")
        assertNotNull(field)
        annotations = field.annotations
        assertEquals(1, annotations.size)

        var forField = annotations[0]
        assertEquals("AnnotatedField", forField.name)

        field = record.getField("anotherField")
        assertNotNull(field)

        annotations = field.annotations
        assertEquals(1, annotations.size)

        forField = annotations[0]
        assertEquals("AnnotatedField", forField.name)

        value = forField.members[0]
        assertEquals(JavaLanguageFrontend.ANNOTATION_MEMBER_VALUE, value.name)
        assertEquals("myString", (value.value as? Literal<*>)?.value)
    }

    @Test
    @Throws(Exception::class)
    fun testChainedCalls() {
        val file = File("src/test/resources/Issue285.java")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val record = tu.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(record)

        val func = record.methods.stream().findFirst().orElse(null)
        assertNotNull(func)
        assertNotNull(func.receiver)

        // make sure, that the type system correctly cleans up these duplicate types
        assertSame(record.getThis()?.type, func.receiver?.type)

        val nodes = SubgraphWalker.flattenAST(record)
        val request =
            nodes
                .stream()
                .filter { node: Node -> (node is VariableDeclaration && "request" == node.name) }
                .map { node: Node? -> node as? VariableDeclaration? }
                .findFirst()
                .orElse(null)
        assertNotNull(request)
        val initializer = request.initializer
        assertNotNull(initializer)
        assertTrue(initializer is MemberCallExpression)

        val call = initializer as? MemberCallExpression
        assertEquals("get", call?.name)
        val staticCall =
            nodes
                .stream()
                .filter { node: Node? -> node is StaticCallExpression }
                .map { node: Node? -> node as? StaticCallExpression? }
                .findFirst()
                .orElse(null)
        assertNotNull(staticCall)
        assertEquals("doSomethingStatic", staticCall.name)
    }

    @Test
    @Throws(Exception::class)
    fun testSuperFieldUsage() {
        val file1 = File("src/test/resources/fix-328/Cat.java")
        val file2 = File("src/test/resources/fix-328/Animal.java")
        val result = analyze(listOf(file1, file2), file1.parentFile.toPath(), true)
        val tu = findByUniqueName(result, "src/test/resources/fix-328/Cat.java")
        val namespace = tu.getDeclarationAs(0, NamespaceDeclaration::class.java)
        assertNotNull(namespace)
        val record = namespace.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(record)
        val constructor = record.constructors[0]
        val op = constructor.getBodyStatementAs(0, BinaryOperator::class.java)
        assertNotNull(op)
        val lhs = op.lhs as? MemberExpression
        val superThisField =
            (lhs?.base as? DeclaredReferenceExpression)?.refersTo as? FieldDeclaration?
        assertNotNull(superThisField)
        assertEquals("this", superThisField.name)
        assertEquals(TypeParser.createFrom("my.Animal", false), superThisField.type)
    }
}
