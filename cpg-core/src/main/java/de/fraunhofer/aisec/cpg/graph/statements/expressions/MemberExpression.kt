/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.HasBase
import de.fraunhofer.aisec.cpg.graph.PopulatedByPass
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.passes.NewResolver
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * Represents access to a declaration of a [RecordDeclaration], such as `obj.property`, in which
 * `property` is a [FieldDeclaration]. It can also be the [CallExpression.callee] of a
 * [MemberCallExpression], in which case this declaration this refers to is a [MethodDeclaration].
 */
class MemberExpression : DeclaredReferenceExpression(), HasBase {

    /**
     * The base expression of this member expression. It must be a valid expression or a
     * [ProblemExpression], if it could not be parsed.
     */
    @field:SubGraph("AST") override var base: Expression? = null

    /**
     * The operator used to access the declaration. This defaults to the dot syntax, such as
     * `obj.property`, but in some languages other codes, such as arrows are used.
     */
    var operatorCode: String? = "."

    /**
     * The record declaration this member expression is point to. This is populated by the
     * [NewResolver].
     */
    @PopulatedByPass(value = NewResolver::class) var record: RecordDeclaration? = null

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("base", base?.name)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MemberExpression) {
            return false
        }
        return super.equals(other) && base == other.base
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
