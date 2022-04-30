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
package de.fraunhofer.aisec.cpg.passes.scopes

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.TypedefDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.Util
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.stream.Collectors

/**
 * Is a scope where local variables can be declared and independent of specific language constructs.
 * Works for if, for, and extends to the block scope
 */
open class ValueDeclarationScope(override var astNode: Node?) : Scope(astNode) {
    var valueDeclarations = mutableListOf<ValueDeclaration>()

    /** A map of typedefs keyed by their alias. */
    val typedefs = mutableMapOf<Type, TypedefDeclaration>()

    fun addTypedef(typedef: TypedefDeclaration) {
        typedefs[typedef.alias] = typedef
    }

    open fun addDeclaration(declaration: Declaration, addToAST: Boolean) {
        if (declaration is ValueDeclaration) {
            addValueDeclaration(declaration, addToAST)
        } else {
            Util.errorWithFileLocation(
                declaration,
                log,
                "A non ValueDeclaration can not be added to a DeclarationScope"
            )
        }
    }

    /**
     * THe value declarations are only set in the ast node if the handler of the ast node may not
     * know the outer
     *
     * @param valueDeclaration the [ValueDeclaration]
     * @param addToAST whether to also add the declaration to the AST of its holder.
     */
    fun addValueDeclaration(valueDeclaration: ValueDeclaration, addToAST: Boolean) {
        valueDeclarations.add(valueDeclaration)
        if (addToAST) {
            if (astNode is DeclarationHolder) {
                val holder = astNode as DeclarationHolder
                holder.addDeclaration(valueDeclaration)
            } else {
                Util.errorWithFileLocation(
                    valueDeclaration,
                    log,
                    "Trying to add a value declaration to a scope which does not have a declaration holder AST node"
                )
            }
        }
        /*
         There are nodes where we do not set the declaration when storing them in the scope,
         mostly for structures that have a single value-declaration: WhileStatement, DoStatement,
         ForStatement, SwitchStatement; and others where the location of declaration is somewhere
         deeper in the AST-subtree: CompoundStatement, AssertStatement.
        */
    }

    companion object {
        @JvmStatic
        protected val log: Logger = LoggerFactory.getLogger(ValueDeclarationScope::class.java)
    }

    override fun resolveSymbol(name: String): List<Declaration> {
        return valueDeclarations
            .filter { declaration: ValueDeclaration ->
                declaration.name.equals(
                    name
                )
            }
    }
}
