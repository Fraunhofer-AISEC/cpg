package de.fraunhofer.aisec.cpg.enhancements

import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ReDeclarableTest {
    @Test
    fun testRedeclare() {
        val tu = TranslationUnitDeclaration()
        val scope = ScopeManager()
        scope.resetToGlobal(tu)

        val a1 = NodeBuilder.newRecordDeclaration("A", "class", "class A;")

        scope.addDeclaration(a1)

        val a2 = NodeBuilder.newRecordDeclaration("A", "class", "class A;")
        a2.setIsDefinition(true)

        scope.addDeclaration(a2)

        Assertions.assertEquals(a1, a2.previous)
        Assertions.assertEquals(a2, a1.previous)
        Assertions.assertEquals(a2, a1.mostRecentDeclaration)
        Assertions.assertEquals(a2, a2.mostRecentDeclaration)
        Assertions.assertEquals(a2, a1.definition)
        Assertions.assertEquals(a2, a2.definition)
    }
}