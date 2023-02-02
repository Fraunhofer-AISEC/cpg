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

import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.Util.distinctBy
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Transient
import java.util.List
import java.util.stream.Collectors

/**
 * A unary operator expression, involving one expression and an operator, such as `a++`.
 */
class UnaryOperator : Expression(), HasType.TypeListener {
    /** The expression on which the operation is applied.  */
    @SubGraph("AST")
    private var input: Expression? = null
    set(value) {
        field?.unregisterTypeListener(this)
        field = value
        input?.registerTypeListener(this)
        changeExpressionAccess()
    }

    /** The operator code.  */
    private var operatorCode: String? = null

    /** Specifies, whether this a post fix operation.  */
    var isPostfix = false

    /** Specifies, whether this a pre fix operation.  */
    var isPrefix = false

    @Transient
    private val checked: MutableList<HasType.TypeListener> = ArrayList()

    private fun changeExpressionAccess() {
        var access = AccessValues.READ
        if (operatorCode == "++" || operatorCode == "--") {
            access = AccessValues.READWRITE
        }
        if (input is DeclaredReferenceExpression) {
            (input as? DeclaredReferenceExpression)?.access = access
        }
    }

    private fun getsDataFromInput(curr: HasType.TypeListener, target: HasType.TypeListener): Boolean {
        val worklist: MutableList<HasType.TypeListener> = ArrayList()
        worklist.add(curr)
        while (!worklist.isEmpty()) {
            val tl = worklist.removeAt(0)
            if (!checked.contains(tl)) {
                checked.add(tl)
                if (tl === target) {
                    return true
                }
                if (curr is HasType) {
                    worklist.addAll((curr as HasType).typeListeners)
                }
            }
        }
        return false
    }

    private fun getsDataFromInput(listener: HasType.TypeListener): Boolean {
        checked.clear()
        if (input == null) return false
        for (l in input!!.typeListeners) {
            if (getsDataFromInput(l, listener)) return true
        }
        return false
    }

    fun getOperatorCode(): String? {
        return operatorCode
    }

    fun setOperatorCode(operatorCode: String?) {
        this.operatorCode = operatorCode
        changeExpressionAccess()
    }

    override fun typeChanged(
        src: HasType, root: MutableList<HasType>, oldType: Type
    ) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        val previous = type
        if (src === input) {
            var newType = src.propagationType
            if (operatorCode == "*") {
                newType = newType.dereference()
            } else if (operatorCode == "&") {
                newType = newType.reference(PointerType.PointerOrigin.POINTER)
            }
            setType(newType, root)
        } else {
            // Our input didn't change, so we don't need to (de)reference the type
            setType(src.propagationType, root)

            // Pass the type on to the input in an inversely (de)referenced way
            var newType: Type? = src.propagationType
            if (operatorCode == "*") {
                newType = src.propagationType.reference(PointerType.PointerOrigin.POINTER)
            } else if (operatorCode == "&") {
                newType = src.propagationType.dereference()
            }

            // We are a fuzzy parser, so while this should not happen, there is no guarantee that input is
            // not null
            if (input != null) {
                input!!.setType(newType!!, ArrayList(List.of(this)))
            }
        }
        if (previous != type) {
            type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: MutableList<HasType>) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        if (src is HasType.TypeListener && getsDataFromInput(src as HasType.TypeListener)) {
            return
        }
        var currSubTypes: MutableList<Type> = ArrayList(possibleSubTypes)
        val newSubTypes = src.possibleSubTypes
        currSubTypes.addAll(newSubTypes)
        if (operatorCode == "*") {
            currSubTypes = currSubTypes.stream()
                .filter(distinctBy { obj: Type -> obj.typeName })
                .map { obj: Type -> obj.dereference() }
                .collect(Collectors.toList())
        } else if (operatorCode == "&") {
            currSubTypes = currSubTypes.stream()
                .filter(distinctBy { obj: Type -> obj.typeName })
                .map { t: Type -> t.reference(PointerType.PointerOrigin.POINTER) }
                .collect(Collectors.toList())
        }
        _possibleSubTypes.clear()
        setPossibleSubTypes(currSubTypes, root) // notify about the new type
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("operatorCode", operatorCode)
            .append("postfix", isPostfix)
            .append("prefix", isPrefix)
            .toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is UnaryOperator) {
            return false
        }
        val that = o
        return super.equals(that) && isPostfix == that.isPostfix && isPrefix == that.isPrefix && input == that.input && operatorCode == that.operatorCode
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    companion object {
        const val OPERATOR_POSTFIX_INCREMENT = "++"
        const val OPERATOR_POSTFIX_DECREMENT = "--"
    }
}