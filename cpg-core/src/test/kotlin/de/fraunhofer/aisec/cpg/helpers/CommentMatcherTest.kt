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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import kotlin.test.*

class CommentMatcherTest {
    @Test
    fun testCommentMatcher() {
        val file = File("src/test/resources/Comments.java")

        val config =
            TranslationConfiguration.builder()
                .sourceLocations(listOf(file))
                .defaultPasses()
                .debugParser(true)
                .defaultLanguages()
                .failOnError(true)
                .build()

        val analyzer = TranslationManager.builder().config(config).build()

        val result = analyzer.analyze().get()
        assertNotNull(result)
        // The class should have 2 comments: The javadoc and "Class comment"
        val tu = result.translationUnits.first()
        val classDeclaration = tu.declarations.first() as RecordDeclaration
        classDeclaration.comment = "" // Reset the comment of the ClassDerclaration

        val comment = "This comment clearly belongs to the class."
        CommentMatcher().matchCommentToNode(comment, Region(2, 4, 2, 46), tu)
        assertTrue(classDeclaration.comment?.contains(comment) == true)

        val comment2 = "Class comment"
        CommentMatcher().matchCommentToNode(comment2, Region(3, 28, 3, 41), tu)
        assertTrue(classDeclaration.comment?.contains(comment2) == true)

        // "javadoc of arg" belongs to the arg and not the class
        val fieldDecl = classDeclaration.declarations.first() as FieldDeclaration
        fieldDecl.comment = ""
        val comment3 = "javadoc of arg"
        CommentMatcher().matchCommentToNode(comment3, Region(5, 9, 5, 23), tu)
        assertTrue(fieldDecl.comment?.contains(comment3) == true)
        assertFalse(classDeclaration.comment?.contains(comment3) == true)

        // 2 line comment in the constructor
        val constructor = classDeclaration.constructors.first()
        val constructorAssignment = (constructor.body as CompoundStatement).statements[0]
        assertNull(constructor.comment)
        constructorAssignment.comment = ""

        val comment4 = "We assign arg to this.arg"
        val comment5 = "The comment needs 2 lines."
        CommentMatcher().matchCommentToNode(comment4, Region(9, 12, 9, 37), tu)
        CommentMatcher().matchCommentToNode(comment5, Region(10, 12, 9, 38), tu)
        assertTrue(constructorAssignment.comment?.contains(comment4) == true)
        assertTrue(constructorAssignment.comment?.contains(comment5) == true)
        assertNull(constructor.comment)

        val mainMethod = classDeclaration.declarations[1] as MethodDeclaration
        assertNull(mainMethod.comment)
        val forLoop = (mainMethod.body as CompoundStatement).statements[0] as ForStatement
        forLoop.comment = null

        val comment6 = "for loop"
        CommentMatcher().matchCommentToNode(comment6, Region(15, 14, 15, 22), tu)
        assertEquals(
            comment6,
            forLoop.comment
        ) // It doesn't put the whole comment, only the part that amtches

        // TODO IMHO the comment "i decl" should belong to the declaration statement of i. But
        // somehow,
        // the comment matcher puts it to the loop condition.
        val comment7 = "i decl"
        CommentMatcher().matchCommentToNode(comment7, Region(16, 26, 16, 32), tu)
        // assertEquals(comment7, forLoop.initializerStatement.comment)

        val printStatement = (forLoop.statement as CompoundStatement).statements.first()
        printStatement.comment = null
        val comment8 = "Crazy print"
        val comment9 = "Comment which belongs to nothing"
        CommentMatcher().matchCommentToNode(comment8, Region(17, 37, 17, 48), tu)
        CommentMatcher().matchCommentToNode(comment9, Region(18, 16, 18, 48), tu)
        assertTrue(printStatement.comment?.contains(comment8) == true)
        // TODO The second comment doesn't belong to the print but to the loop body
        assertTrue(forLoop.statement.comment?.contains(comment9) == true)

        assertNull(mainMethod.comment)
    }
}
