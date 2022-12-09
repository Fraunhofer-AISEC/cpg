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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.frontends.HasSuperClasses
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.CallResolver.Companion.LOGGER

/**
 * Handle calls in the form of `super.call()` or `ClassName.super.call() ` * , conforming to JLS13
 * §15.12.1
 *
 * @param curClass The class containing the call
 * @param call The call to be resolved
 */
fun CallResolver.handleSuperCall(curClass: RecordDeclaration, call: CallExpression) {
    // We need to connect this super reference to the receiver of this method
    val func = scopeManager.currentFunction
    if (func is MethodDeclaration) {
        (call.base as DeclaredReferenceExpression?)?.refersTo = func.receiver
    }
    var target: RecordDeclaration? = null
    if (call.base?.fullName.toString() == JavaLanguage().superClassKeyword) {
        // Direct superclass, either defined explicitly or java.lang.Object by default
        if (curClass.superClasses.isNotEmpty()) {
            target = recordMap[curClass.superClasses[0].root.fullName]
        } else {
            Util.warnWithFileLocation(
                call,
                LOGGER,
                "super call without direct superclass! Expected java.lang.Object to be present at least!"
            )
        }
    } else {
        // BaseName.super.call(), might either be in order to specify an enclosing class or an
        // interface that is implemented
        target = handleSpecificSupertype(curClass, call)
    }
    if (target != null) {
        val superType = target.toType()
        // Explicitly set the type of the call's base to the super type
        call.base!!.type = superType
        // And set the possible subtypes, to ensure, that really only our super type is in there
        call.base!!.updatePossibleSubtypes(listOf(superType))
        handleMethodCall(target, call)
    }
}

fun CallResolver.handleSpecificSupertype(
    curClass: RecordDeclaration,
    call: CallExpression
): RecordDeclaration? {
    // TODO: Somehow, the old expression looks as if the super could be somewhere in the middle of
    // the name. I think this doesn't make much sense. If that was not the intention, just remove
    // the while loop.
    var baseFullName = call.base?.fullName
    while (
        baseFullName != null &&
            baseFullName.localName != (curClass.language as HasSuperClasses).superClassKeyword
    ) {
        baseFullName = baseFullName.parent
    }
    if (baseFullName?.localName == (curClass.language as HasSuperClasses).superClassKeyword) {
        baseFullName = baseFullName.parent
    }
    baseFullName = baseFullName ?: call.base!!.fullName
    val baseName = baseFullName

    // val baseName = call.base!!.name.substring(0,
    // call.base!!.fullName.toString().lastIndexOf(".super"))
    if (TypeParser.createFrom(baseName, curClass.language) in curClass.implementedInterfaces) {
        // Basename is an interface -> BaseName.super refers to BaseName itself
        return recordMap[baseName]
    } else {
        // BaseName refers to an enclosing class -> BaseName.super is BaseName's superclass
        val base = recordMap[baseName]
        if (base != null) {
            if (base.superClasses.isNotEmpty()) {
                return recordMap[base.superClasses[0].root.fullName]
            } else {
                Util.warnWithFileLocation(
                    call,
                    LOGGER,
                    "super call without direct superclass! Expected java.lang.Object to be present at least!"
                )
            }
        }
    }
    return null
}
