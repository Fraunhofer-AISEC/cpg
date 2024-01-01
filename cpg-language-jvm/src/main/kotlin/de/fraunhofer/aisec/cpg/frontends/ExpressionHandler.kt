/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import sootup.core.jimple.basic.Local
import sootup.core.jimple.basic.Value
import sootup.core.jimple.common.constant.DoubleConstant
import sootup.core.jimple.common.constant.FloatConstant
import sootup.core.jimple.common.constant.IntConstant
import sootup.core.jimple.common.constant.LongConstant
import sootup.core.jimple.common.constant.StringConstant
import sootup.core.jimple.common.expr.JAddExpr
import sootup.core.jimple.common.expr.JNewExpr
import sootup.core.jimple.common.expr.JSpecialInvokeExpr
import sootup.core.jimple.common.expr.JStaticInvokeExpr
import sootup.core.jimple.common.expr.JVirtualInvokeExpr
import sootup.core.jimple.common.ref.JParameterRef
import sootup.core.jimple.common.ref.JStaticFieldRef
import sootup.core.jimple.common.ref.JThisRef
import sootup.core.signatures.MethodSignature
import sootup.java.core.jimple.basic.JavaLocal

class ExpressionHandler(frontend: JVMLanguageFrontend) :
    Handler<Expression, Value, JVMLanguageFrontend>(
        ::ProblemExpression,
        frontend,
    ) {

    init {
        map.put(Local::class.java) { handleLocal(it as Local) }
        map.put(JavaLocal::class.java) { handleLocal(it as Local) }
        map.put(JThisRef::class.java) { handleThisRef(it as JThisRef) }
        map.put(JParameterRef::class.java) { handleParameterRef(it as JParameterRef) }
        map.put(JStaticFieldRef::class.java) { handleStaticFieldRef(it as JStaticFieldRef) }
        map.put(JVirtualInvokeExpr::class.java) {
            handleVirtualInvokeExpr(it as JVirtualInvokeExpr)
        }
        map.put(JSpecialInvokeExpr::class.java) { handleSpecialInvoke(it as JSpecialInvokeExpr) }
        map.put(JStaticInvokeExpr::class.java) { handleStaticInvoke(it as JStaticInvokeExpr) }
        map.put(JAddExpr::class.java) { handleAddExpr(it as JAddExpr) }
        map.put(JNewExpr::class.java) { handleNewExpr(it as JNewExpr) }
        map.put(FloatConstant::class.java) { handleFloatConstant(it as FloatConstant) }
        map.put(DoubleConstant::class.java) { handleDoubleConstant(it as DoubleConstant) }
        map.put(IntConstant::class.java) { handleIntConstant(it as IntConstant) }
        map.put(LongConstant::class.java) { handleLongConstant(it as LongConstant) }
        map.put(StringConstant::class.java) { handleStringConstant(it as StringConstant) }
    }

    private fun handleLocal(local: Local): Expression {
        // Apparently, a local can either be a reference to variable or a literal
        return if (local.name.startsWith("\"")) {
            val lit = newLiteral(local.name.substring(1, local.name.length - 2), rawNode = local)
            lit.type = objectType("java.lang.String")

            lit
        } else {
            val ref = newReference(local.name, rawNode = local)

            ref
        }
    }

    private fun handleThisRef(thisRef: JThisRef): Reference {
        val ref = newReference("@this", frontend.typeOf(thisRef.type), rawNode = thisRef)

        return ref
    }

    private fun handleParameterRef(parameterRef: JParameterRef): Reference {
        val ref =
            newReference(
                "@parameter${parameterRef.index}",
                frontend.typeOf(parameterRef.type),
                rawNode = parameterRef
            )

        return ref
    }

    private fun handleStaticFieldRef(staticFieldRef: JStaticFieldRef): Reference {
        // TODO(oxisto): not sure if this shouldn't be a regular reference instead
        val base =
            newReference(
                staticFieldRef.fieldSignature.declClassType.fullyQualifiedName,
                frontend.typeOf(staticFieldRef.fieldSignature.declClassType)
            )

        val expr =
            newMemberExpression(
                staticFieldRef.fieldSignature.name,
                base,
                frontend.typeOf(staticFieldRef.type),
                rawNode = staticFieldRef
            )
        expr.isStaticAccess = true

        return expr
    }

    private fun handleVirtualInvokeExpr(
        virtualInvokeExpr: JVirtualInvokeExpr
    ): MemberCallExpression {
        val base = handle(virtualInvokeExpr.base) ?: newProblemExpression("could not parse base")
        // Not really necessary, but since we already have the type information, we can use it
        base.type = frontend.typeOf(virtualInvokeExpr.methodSignature.declClassType)

        val callee = newMemberExpression(virtualInvokeExpr.methodSignature.name, base)

        val call = newMemberCallExpression(callee, rawNode = virtualInvokeExpr)
        call.arguments = virtualInvokeExpr.args.mapNotNull { handle(it) }

        return call
    }

    fun handleSpecialInvoke(specialInvokeExpr: JSpecialInvokeExpr): Expression {
        // This is probably a constructor call or another corner case
        return if (specialInvokeExpr.methodSignature.name == "<init>") {
            val type = frontend.typeOf(specialInvokeExpr.methodSignature.declClassType)
            val construct = newConstructExpression(rawNode = specialInvokeExpr)
            construct.callee = newReference(Name("<init>", type.name))
            construct.type = type

            construct.arguments = specialInvokeExpr.args.mapNotNull { handle(it) }

            construct
        } else {
            newProblemExpression("specialinvoke with something unknown")
        }
    }

    fun handleStaticInvoke(staticInvokeExpr: JStaticInvokeExpr): CallExpression {
        val ref = staticInvokeExpr.methodSignature.toStaticRef()

        val call = newCallExpression(ref, rawNode = staticInvokeExpr)
        call.arguments = staticInvokeExpr.args.mapNotNull { handle(it) }
        call.type = frontend.typeOf(staticInvokeExpr.type)

        return call
    }

    fun handleAddExpr(addExpr: JAddExpr): BinaryOperator {
        val op = newBinaryOperator("+", rawNode = addExpr)
        handle(addExpr.op1)?.let { op.lhs = it }
        handle(addExpr.op2)?.let { op.rhs = it }

        return op
    }

    fun handleNewExpr(newExpr: JNewExpr): NewExpression {
        val type = frontend.typeOf(newExpr.type)
        val new = newNewExpression(type, rawNode = newExpr)

        // In the jimple IR, the "new" and the constructor calls are split into two expressions.
        // This will only handle the "new" expression, a later call to "invokespecial" will handle
        // the constructor call.

        return new
    }

    fun handleFloatConstant(constant: FloatConstant): Literal<Float> {
        return newLiteral(constant.value, primitiveType("float"), rawNode = constant)
    }

    fun handleDoubleConstant(constant: DoubleConstant): Literal<Double> {
        return newLiteral(constant.value, primitiveType("double"), rawNode = constant)
    }

    fun handleIntConstant(constant: IntConstant): Literal<Int> {
        return newLiteral(constant.value, primitiveType("int"), rawNode = constant)
    }

    fun handleLongConstant(constant: LongConstant): Literal<Long> {
        return newLiteral(constant.value, primitiveType("long"), rawNode = constant)
    }

    fun handleStringConstant(constant: StringConstant): Literal<String> {
        return newLiteral(constant.value, primitiveType("java.lang.String"), rawNode = constant)
    }

    private fun MethodSignature.toStaticRef(): Reference {
        // First, construct the name using <parent-type>.<fun>
        val ref = newReference("${this.declClassType.fullyQualifiedName}.${this.name}")

        // We can also provide a function type, since these are all statically known. This might
        // help in inferring some (unknown) functions later
        ref.type =
            FunctionType(
                this.name,
                this.parameterTypes.map { frontend.typeOf(it) },
                listOf(frontend.typeOf(this.type)),
                frontend.language
            )

        // Make it static
        ref.isStaticAccess = true

        return ref
    }
}
