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
package de.fraunhofer.aisec.cpg.frontends.java

import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.SymbolResolver.Companion.LOGGER

/**
 * Handle expressions in the form of `super.property` or `ClassName.super.call()`, conforming to
 * JLS13 §15.12.1.
 *
 * This function basically sets the correct type of the [Reference] containing the "super" keyword.
 *
 * @param memberExpression The member expression that needs to be adjusted
 * @param curClass The class containing the call
 */
fun SymbolResolver.handleSuperExpressionHelper(
    memberExpression: MemberExpression,
    curClass: Record,
): Boolean {
    // Because the "super" keyword still refers to "this" (but cast to another class), we
    // still need to connect the super reference to the receiver of this method.
    val func = scopeManager.currentFunction
    if (func is Method) {
        (memberExpression.base as Reference?)?.refersTo = func.receiver
    }

    // In the next step we can "cast" the base to the correct type, by setting the base
    var target: Record? = null

    // In case the reference is just called "super", this is a direct superclass, either
    // defined explicitly or java.lang.Object by default
    if (
        memberExpression.base.name.toString() ==
            (memberExpression.language as? JavaLanguage)?.superClassKeyword
    ) {
        if (curClass.superClasses.isNotEmpty()) {
            target = curClass.superClasses[0].root.recordDeclaration
        } else {
            Util.warnWithFileLocation(
                memberExpression,
                LOGGER,
                "super call without direct superclass! Expected java.lang.Object to be present at least!",
            )
        }
    } else {
        // BaseName.super.call(), might either be in order to specify an enclosing class or
        // an interface that is implemented
        target = handleSpecificSupertype(memberExpression, curClass)
    }

    if (target != null) {
        val superType = target.toType()
        // Explicitly set the type of the call's base to the super type, basically "casting"
        // the "this" object to the super class
        memberExpression.base.type = superType

        val refersTo = (memberExpression.base as? Reference)?.refersTo
        if (refersTo is HasType) {
            refersTo.type = superType
            refersTo.assignedTypes = mutableSetOf(superType)
        }

        // Make sure that really only our super class is in the list of assigned types
        memberExpression.base.assignedTypes = mutableSetOf(superType)

        return true
    }

    return false
}

fun SymbolResolver.handleSpecificSupertype(
    callee: MemberExpression,
    curClass: Record,
): Record? {
    val baseName = callee.base.name.parent ?: return null

    val type = typeManager.lookupResolvedType(baseName.toString()) ?: callee.unknownType()
    if (type in curClass.implementedInterfaces) {
        // Basename is an interface -> BaseName.super refers to BaseName itself
        return type.recordDeclaration
    } else {
        // BaseName refers to an enclosing class -> BaseName.super is BaseName's superclass
        val base = type.recordDeclaration
        if (base != null) {
            if (base.superClasses.isNotEmpty()) {
                return base.superClasses[0].root.recordDeclaration
            } else {
                Util.warnWithFileLocation(
                    callee,
                    LOGGER,
                    "super call without direct superclass! Expected java.lang.Object to be present at least!",
                )
            }
        }
    }

    return null
}
