/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.jvm

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import sootup.core.jimple.basic.Local
import sootup.core.jimple.basic.Value
import sootup.core.jimple.common.constant.*
import sootup.core.jimple.common.expr.*
import sootup.core.jimple.common.ref.*
import sootup.core.signatures.MethodSignature
import sootup.core.signatures.SootClassMemberSignature
import sootup.java.core.jimple.basic.JavaLocal

class ExpressionHandler(frontend: JVMLanguageFrontend) :
    Handler<Expression, Value, JVMLanguageFrontend>(::ProblemExpression, frontend) {

    init {
        map.put(JCaughtExceptionRef::class.java) { handleExceptionRef(it as JCaughtExceptionRef) }
        map.put(Local::class.java) { handleLocal(it as Local) }
        map.put(JavaLocal::class.java) { handleLocal(it as Local) }
        map.put(JThisRef::class.java) { handleThisRef(it as JThisRef) }
        map.put(JParameterRef::class.java) { handleParameterRef(it as JParameterRef) }
        map.put(JInstanceFieldRef::class.java) { handleInstanceFieldRef(it as JInstanceFieldRef) }
        map.put(JStaticFieldRef::class.java) { handleStaticFieldRef(it as JStaticFieldRef) }
        map.put(JArrayRef::class.java) { handleArrayRef(it as JArrayRef) }
        map.put(JInterfaceInvokeExpr::class.java) {
            handleInterfaceInvokeExpr(it as JInterfaceInvokeExpr)
        }
        map.put(JVirtualInvokeExpr::class.java) {
            handleVirtualInvokeExpr(it as JVirtualInvokeExpr)
        }
        map.put(JDynamicInvokeExpr::class.java) {
            handleDynamicInvokeExpr(it as JDynamicInvokeExpr)
        }
        map.put(JSpecialInvokeExpr::class.java) { handleSpecialInvoke(it as JSpecialInvokeExpr) }
        map.put(JStaticInvokeExpr::class.java) { handleStaticInvoke(it as JStaticInvokeExpr) }
        map.put(JNewExpr::class.java) { handleNewExpr(it as JNewExpr) }
        map.put(JNewArrayExpr::class.java) { handleNewArrayExpr(it as JNewArrayExpr) }
        map.put(JNewMultiArrayExpr::class.java) {
            handleNewMultiArrayExpr(it as JNewMultiArrayExpr)
        }
        map.put(JCastExpr::class.java) { handleCastExpr(it as JCastExpr) }

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

        // Fallback, just to be sure
        map.put(AbstractBinopExpr::class.java) { handleAbstractBinopExpr(it as AbstractBinopExpr) }

        // Unary operator
        map.put(JNegExpr::class.java) { handleNegExpr(it as JNegExpr) }

        // Special operators, which we need to model as binary/unary operators
        map.put(JInstanceOfExpr::class.java) { handleInstanceOfExpr(it as JInstanceOfExpr) }
        map.put(JLengthExpr::class.java) { handleLengthExpr(it as JLengthExpr) }

        // Constants
        map.put(BooleanConstant::class.java) { handleBooleanConstant(it as BooleanConstant) }
        map.put(FloatConstant::class.java) { handleFloatConstant(it as FloatConstant) }
        map.put(DoubleConstant::class.java) { handleDoubleConstant(it as DoubleConstant) }
        map.put(IntConstant::class.java) { handleIntConstant(it as IntConstant) }
        map.put(LongConstant::class.java) { handleLongConstant(it as LongConstant) }
        map.put(StringConstant::class.java) { handleStringConstant(it as StringConstant) }
        map.put(NullConstant::class.java) { handleNullConstant(it as NullConstant) }
        map.put(ClassConstant::class.java) { handleClassConstant(it as ClassConstant) }
    }

    private fun handleExceptionRef(exceptionRef: JCaughtExceptionRef): Expression {
        return newReference(name = "@caughtexception").apply {
            this.type = frontend.typeOf(exceptionRef.type)
        }
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
                rawNode = parameterRef,
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
                rawNode = instanceFieldRef,
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

    private fun handleAbstractInstanceInvokeExpr(
        invokeExpr: AbstractInstanceInvokeExpr
    ): MemberCallExpression {
        val base = handle(invokeExpr.base) ?: newProblemExpression("could not parse base")
        // Not really necessary, but since we already have the type information, we can use it
        base.type = frontend.typeOf(invokeExpr.methodSignature.declClassType)

        val callee = newMemberExpression(invokeExpr.methodSignature.name, base)

        val call = newMemberCallExpression(callee, rawNode = invokeExpr)
        call.arguments = invokeExpr.args.mapNotNull { handle(it) }.toMutableList()

        return call
    }

    private fun handleVirtualInvokeExpr(invokeExpr: JVirtualInvokeExpr): MemberCallExpression {
        return handleAbstractInstanceInvokeExpr(invokeExpr)
    }

    private fun handleInterfaceInvokeExpr(invokeExpr: JInterfaceInvokeExpr): MemberCallExpression {
        return handleAbstractInstanceInvokeExpr(invokeExpr)
    }

    /**
     * The difference between [JSpecialInvokeExpr] and a regular [JVirtualInvokeExpr] is that the
     * invoked function is not part of the declared class, but rather it is a function of its base
     * class(es).
     *
     * We currently can only model this as a regular call and hope that the [SymbolResolver] will
     * pick the correct function. Maybe we can supply some kind of hint to the resolver to make this
     * better.
     */
    private fun handleSpecialInvoke(invokeExpr: JSpecialInvokeExpr): Expression {
        // This is probably a constructor call
        return if (invokeExpr.methodSignature.name == "<init>") {
            val type = frontend.typeOf(invokeExpr.methodSignature.declClassType)
            val construct = newConstructExpression(rawNode = invokeExpr)
            construct.callee = newReference(Name("<init>", type.name))
            construct.type = type

            construct.arguments = invokeExpr.args.mapNotNull { handle(it) }.toMutableList()

            construct
        } else {
            // Just a normal call
            return handleAbstractInstanceInvokeExpr(invokeExpr)
        }
    }

    private fun handleDynamicInvokeExpr(dynamicInvokeExpr: AbstractInvokeExpr): CallExpression {
        // Model this as a static call to the method. Not sure if this is really that good or if we
        // want to somehow "call" the underlying bootstrap method.
        // TODO(oxisto): This is actually somewhat related to a LambdaExpression, but not really
        // sure ow to model this
        val callee = dynamicInvokeExpr.methodSignature.toStaticRef()
        val call = newCallExpression(callee, rawNode = dynamicInvokeExpr)
        call.arguments = dynamicInvokeExpr.args.mapNotNull { handle(it) }.toMutableList()
        call.type = frontend.typeOf(dynamicInvokeExpr.methodSignature.type)

        return call
    }

    private fun handleStaticInvoke(staticInvokeExpr: JStaticInvokeExpr): CallExpression {
        val ref = staticInvokeExpr.methodSignature.toStaticRef()

        val call = newCallExpression(ref, rawNode = staticInvokeExpr)
        call.arguments = staticInvokeExpr.args.mapNotNull { handle(it) }.toMutableList()
        call.type = frontend.typeOf(staticInvokeExpr.type)

        return call
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
        new.dimensions = listOfNotNull(handle(newArrayExpr.size)).toMutableList()

        return new
    }

    private fun handleNewMultiArrayExpr(newMultiArrayExpr: JNewMultiArrayExpr): NewArrayExpression {
        val new = newNewArrayExpression(rawNode = newMultiArrayExpr)
        new.type = frontend.typeOf(newMultiArrayExpr.type)
        new.dimensions = newMultiArrayExpr.sizes.mapNotNull { handle(it) }.toMutableList()

        return new
    }

    private fun handleCastExpr(castExpr: JCastExpr): CastExpression {
        val cast = newCastExpression(rawNode = castExpr)
        cast.expression = handle(castExpr.op) ?: newProblemExpression("missing expression")
        cast.castType = frontend.typeOf(castExpr.type)

        return cast
    }

    private fun handleAbstractBinopExpr(expr: AbstractBinopExpr): BinaryOperator {
        val op = newBinaryOperator(expr.symbol.trim(), rawNode = expr)
        op.lhs = handle(expr.op1) ?: newProblemExpression("missing lhs")
        op.rhs = handle(expr.op2) ?: newProblemExpression("missing rhs")
        op.type = frontend.typeOf(expr.type)

        return op
    }

    private fun handleNegExpr(expr: AbstractUnopExpr): UnaryOperator {
        val op = newUnaryOperator("-", postfix = false, prefix = true, rawNode = expr)
        op.input = handle(expr.op) ?: newProblemExpression("missing input")
        op.type = frontend.typeOf(expr.type)

        return op
    }

    private fun handleInstanceOfExpr(instanceOfExpr: JInstanceOfExpr): BinaryOperator {
        val op = newBinaryOperator("instanceof", rawNode = instanceOfExpr)
        op.lhs = handle(instanceOfExpr.op) ?: newProblemExpression("missing lhs")

        val type = frontend.typeOf(instanceOfExpr.checkType)
        op.rhs = newTypeExpression("", type, rawNode = type)
        op.rhs.name = type.name
        op.type = frontend.typeOf(instanceOfExpr.type)

        return op
    }

    private fun handleLengthExpr(lengthExpr: JLengthExpr): UnaryOperator {
        val op = newUnaryOperator("lengthof", prefix = true, postfix = false, rawNode = lengthExpr)
        op.input = handle(lengthExpr.op) ?: newProblemExpression("missing input")
        op.type = frontend.typeOf(lengthExpr.type)

        return op
    }

    private fun handleBooleanConstant(constant: BooleanConstant) =
        newLiteral(
            constant.equalEqual(BooleanConstant.getTrue()),
            primitiveType("boolean"),
            rawNode = constant,
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

    /**
     * We need to keep the class name as a string, rather than a [Class], because otherwise we would
     * try to find the specified class on the classpath, which can lead to unwanted results.
     */
    private fun handleClassConstant(constant: ClassConstant) =
        newLiteral(constant.value, primitiveType("java.lang.Class"), rawNode = constant)

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
                frontend.language,
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
