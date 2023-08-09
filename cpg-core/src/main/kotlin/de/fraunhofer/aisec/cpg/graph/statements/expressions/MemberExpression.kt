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

import de.fraunhofer.aisec.cpg.graph.AST
import de.fraunhofer.aisec.cpg.graph.HasBase
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.fqn
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * Represents access to a member of a [RecordDeclaration], such as `obj.property`. Another common
 * use-case is access of a member function (method) as part of the [MemberCallExpression.callee]
 * property of a [MemberCallExpression].
 */
class MemberExpression : DeclaredReferenceExpression(), HasBase {
    @AST
    override var base: Expression = ProblemExpression("could not parse base expression")
        set(value) {
            field.unregisterTypeObserver(this)
            field = value
            updateName()
            value.registerTypeObserver(this)
        }

    override var operatorCode: String? = null

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("base", base)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemberExpression) return false
        return super.equals(other) && base == other.base
    }

    override fun hashCode() = Objects.hash(super.hashCode(), base)

    override fun typeChanged(newType: Type, src: HasType) {
        // We are basically only interested in type changes from our base to update the naming. We
        // need to ignore actual changes to the type because otherwise things go horribly wrong
        if (src == base) {
            updateName()
        } else {
            super.typeChanged(newType, src)
        }
    }

    private fun updateName() {
        this.name = base.type.root.name.fqn(name.localName)
    }
}
