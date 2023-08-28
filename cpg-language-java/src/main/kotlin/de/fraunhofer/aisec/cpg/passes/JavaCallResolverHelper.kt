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

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDecl
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDecl
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpr
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpr
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.CallResolver.Companion.LOGGER

class JavaCallResolverHelper {

    companion object {
        /**
         * Handle calls in the form of `super.call()` or `ClassName.super.call()`, conforming to
         * JLS13 §15.12.1.
         *
         * This function basically sets the correct type of the [Reference] containing the "super"
         * keyword. Afterwards, we can use the regular [CallResolver.resolveMemberCallee] to resolve
         * the [MemberCallExpr].
         *
         * @param callee The callee of the call expression that needs to be adjusted
         * @param curClass The class containing the call
         */
        fun handleSuperCall(
            callee: MemberExpr,
            curClass: RecordDecl,
            scopeManager: ScopeManager,
            recordMap: Map<Name, RecordDecl>
        ): Boolean {
            // Because the "super" keyword still refers to "this" (but casted to another class), we
            // still
            // need to connect the super reference to the receiver of this method.
            val func = scopeManager.currentFunction
            if (func is MethodDecl) {
                (callee.base as Reference?)?.refersTo = func.receiver
            }

            // In the next step we can "cast" the base to the correct type, by setting the base
            var target: RecordDecl? = null

            // In case the reference is just called "super", this is a direct superclass, either
            // defined
            // explicitly or java.lang.Object by default
            if (callee.base.name.toString() == JavaLanguage().superClassKeyword) {
                if (curClass.superClasses.isNotEmpty()) {
                    target = recordMap[curClass.superClasses[0].root.name]
                } else {
                    Util.warnWithFileLocation(
                        callee,
                        LOGGER,
                        "super call without direct superclass! Expected java.lang.Object to be present at least!"
                    )
                }
            } else {
                // BaseName.super.call(), might either be in order to specify an enclosing class or
                // an
                // interface that is implemented
                target = handleSpecificSupertype(callee, curClass, recordMap)
            }

            if (target != null) {
                val superType = target.toType()
                // Explicitly set the type of the call's base to the super type, basically "casting"
                // the "this" object to the super class
                callee.base.type = superType

                val refersTo = (callee.base as? Reference)?.refersTo
                if (refersTo is HasType) {
                    refersTo.type = superType
                    refersTo.assignedTypes = mutableSetOf(superType)
                }

                // Make sure that really only our super class is in the list of assigned types
                callee.base.assignedTypes = mutableSetOf(superType)

                return true
            }

            return false
        }

        fun handleSpecificSupertype(
            callee: MemberExpr,
            curClass: RecordDecl,
            recordMap: Map<Name, RecordDecl>
        ): RecordDecl? {
            val baseName = callee.base.name.parent ?: return null

            if (curClass.objectType(baseName) in curClass.implementedInterfaces) {
                // Basename is an interface -> BaseName.super refers to BaseName itself
                return recordMap[baseName]
            } else {
                // BaseName refers to an enclosing class -> BaseName.super is BaseName's superclass
                val base = recordMap[baseName]
                if (base != null) {
                    if (base.superClasses.isNotEmpty()) {
                        return recordMap[base.superClasses[0].root.name]
                    } else {
                        Util.warnWithFileLocation(
                            callee,
                            LOGGER,
                            "super call without direct superclass! Expected java.lang.Object to be present at least!"
                        )
                    }
                }
            }

            return null
        }
    }
}
