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
import sootup.core.jimple.common.constant.*
import sootup.core.jimple.common.expr.*
import sootup.core.jimple.common.ref.*
import sootup.core.signatures.MethodSignature
import sootup.core.signatures.SootClassMemberSignature
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
        map.put(JInstanceFieldRef::class.java) { handleInstanceFieldRef(it as JInstanceFieldRef) }
        map.put(JStaticFieldRef::class.java) { handleStaticFieldRef(it as JStaticFieldRef) }
        map.put(JArrayRef::class.java) { handleArrayRef(it as JArrayRef) }
        map.put(JVirtualInvokeExpr::class.java) {
            handleVirtualInvokeExpr(it as JVirtualInvokeExpr)
        }
        map.put(JSpecialInvokeExpr::class.java) { handleSpecialInvoke(it as JSpecialInvokeExpr) }
        map.put(JStaticInvokeExpr::class.java) { handleStaticInvoke(it as JStaticInvokeExpr) }
        map.put(JNewExpr::class.java) { handleNewExpr(it as JNewExpr) }
        map.put(JNewArrayExpr::class.java) { handleNewArrayExpr(it as JNewArrayExpr) }

        // Binary operators
        // - Equality checks
        map.put(JEqExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JNeExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JGeExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JGtExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JLeExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JLtExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }

        // - Numeric comparisons
        map.put(JCmpExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JCmplExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JCmpgExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }

        // - Simple arithmetics
        map.put(JAddExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JDivExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JMulExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JRemExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JSubExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }

        // - Binary arithmetics
        map.put(JAndExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JOrExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JShlExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JShrExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JUshrExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }
        map.put(JXorExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }

        // Unary operator
        map.put(JNegExpr::class.java) {
            handleAbstractUnopExpr(
                it as AbstractUnopExpr,
                postfix = false,
                prefix = true,
                opCode = "-"
            )
        }

        // Constants
        map.put(BooleanConstant::class.java) { handleBooleanConstant(it as BooleanConstant) }
        map.put(FloatConstant::class.java) { handleFloatConstant(it as FloatConstant) }
        map.put(DoubleConstant::class.java) { handleDoubleConstant(it as DoubleConstant) }
        map.put(IntConstant::class.java) { handleIntConstant(it as IntConstant) }
        map.put(LongConstant::class.java) { handleLongConstant(it as LongConstant) }
        map.put(StringConstant::class.java) { handleStringConstant(it as StringConstant) }
        map.put(NullConstant::class.java) { handleNullConstant(it as NullConstant) }
    }

    private fun handleLocal(local: Local): Expression {
        // Apparently, a local can either be a reference to variable or a literal
        return if (local.name.startsWith("\"")) {
            val lit = newLiteral(local.name.substring(1, local.name.length - 2), rawNode = local)
            lit.type = objectType("java.lang.String")

            lit
        } else {
            val ref = newReference(local.name, frontend.typeOf(local.type), rawNode = local)

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

    private fun handleInstanceFieldRef(instanceFieldRef: JInstanceFieldRef): Reference {
        val base = handle(instanceFieldRef.base) ?: newProblemExpression("missing base")

        val ref =
            newMemberExpression(
                instanceFieldRef.fieldSignature.name,
                base,
                frontend.typeOf(instanceFieldRef.fieldSignature.type),
                rawNode = instanceFieldRef
            )

        return ref
    }

    private fun handleStaticFieldRef(staticFieldRef: JStaticFieldRef) =
        staticFieldRef.fieldSignature.toStaticRef()

    private fun handleArrayRef(arrayRef: JArrayRef): SubscriptExpression {
        val sub = newSubscriptExpression(rawNode = arrayRef)
        sub.arrayExpression = handle(arrayRef.base) ?: newProblemExpression("missing base")
        sub.subscriptExpression = handle(arrayRef.index) ?: newProblemExpression("missing index")

        return sub
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

    private fun handleSpecialInvoke(specialInvokeExpr: JSpecialInvokeExpr): Expression {
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

    private fun handleStaticInvoke(staticInvokeExpr: JStaticInvokeExpr): CallExpression {
        val ref = staticInvokeExpr.methodSignature.toStaticRef()

        val call = newCallExpression(ref, rawNode = staticInvokeExpr)
        call.arguments = staticInvokeExpr.args.mapNotNull { handle(it) }
        call.type = frontend.typeOf(staticInvokeExpr.type)

        return call
    }

    private fun handleAddExpr(addExpr: JAddExpr): BinaryOperator {
        val op = newBinaryOperator("+", rawNode = addExpr)
        handle(addExpr.op1)?.let { op.lhs = it }
        handle(addExpr.op2)?.let { op.rhs = it }

        return op
    }

    /**
     * In the jimple IR, the "new" and the constructor calls are split into two expressions. This
     * will only handle the "new" expression, a later call to "invokespecial" will handle the
     * constructor call.
     */
    private fun handleNewExpr(newExpr: JNewExpr) =
        newNewExpression(frontend.typeOf(newExpr.type), rawNode = newExpr)

    private fun handleNewArrayExpr(newArrayExpr: JNewArrayExpr): NewArrayExpression {
        val new = newNewArrayExpression(rawNode = newArrayExpr)
        new.type = frontend.typeOf(newArrayExpr.type)
        new.dimensions = listOfNotNull(handle(newArrayExpr.size))

        return new
    }

    private fun handleAbstractBinopExpr(expr: AbstractBinopExpr): BinaryOperator {
        val op = newBinaryOperator(expr.symbol.trim(), rawNode = expr)
        op.lhs = handle(expr.op1) ?: newProblemExpression("missing lhs")
        op.rhs = handle(expr.op2) ?: newProblemExpression("missing rhs")
        op.type = frontend.typeOf(expr.type)

        return op
    }

    private fun handleAbstractUnopExpr(
        expr: AbstractUnopExpr,
        postfix: Boolean,
        prefix: Boolean,
        opCode: String
    ): UnaryOperator {
        val op = newUnaryOperator(opCode, postfix = postfix, prefix = prefix, rawNode = expr)
        op.input = handle(expr.op) ?: newProblemExpression("missing input")
        op.type = frontend.typeOf(expr.type)

        return op
    }

    private fun handleBooleanConstant(constant: BooleanConstant) =
        newLiteral(
            constant.equalEqual(BooleanConstant.getTrue()),
            primitiveType("boolean"),
            rawNode = constant
        )

    private fun handleFloatConstant(constant: FloatConstant) =
        newLiteral(constant.value, primitiveType("float"), rawNode = constant)

    private fun handleDoubleConstant(constant: DoubleConstant) =
        newLiteral(constant.value, primitiveType("double"), rawNode = constant)

    private fun handleIntConstant(constant: IntConstant) =
        newLiteral(constant.value, primitiveType("int"), rawNode = constant)

    private fun handleLongConstant(constant: LongConstant) =
        newLiteral(constant.value, primitiveType("long"), rawNode = constant)

    private fun handleStringConstant(constant: StringConstant) =
        newLiteral(constant.value, primitiveType("java.lang.String"), rawNode = constant)

    private fun handleNullConstant(constant: NullConstant) =
        newLiteral(null, unknownType(), rawNode = constant)

    private fun MethodSignature.toStaticRef(): Reference {
        // First, construct the name using <parent-type>.<fun>
        val ref = (this as SootClassMemberSignature<*>).toStaticRef()

        // We can also provide a function type, since these are all statically known. This might
        // help in inferring some (unknown) functions later
        ref.type =
            FunctionType(
                this.name,
                this.parameterTypes.map { frontend.typeOf(it) },
                listOf(frontend.typeOf(this.type)),
                frontend.language
            )

        return ref
    }

    private fun SootClassMemberSignature<*>.toStaticRef(): Reference {
        // First, construct the name using <parent-type>.<fun>
        val ref = newReference("${this.declClassType.fullyQualifiedName}.${this.name}")

        // Make it static
        ref.isStaticAccess = true

        return ref
    }
}
