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
import de.fraunhofer.aisec.cpg.graph.declarations.*
import sootup.core.jimple.basic.Local
import sootup.core.model.SootClass
import sootup.core.model.SootField
import sootup.core.model.SootMethod

class DeclarationHandler(frontend: JVMLanguageFrontend) :
    Handler<Declaration, Any, JVMLanguageFrontend>(::ProblemDeclaration, frontend) {

    override fun handle(ctx: Any): Declaration {
        try {
            return when (ctx) {
                is SootClass -> handleClass(ctx)
                is SootMethod -> handleMethod(ctx)
                is SootField -> handleField(ctx)
                is Local -> handleLocal(ctx)
                else -> {
                    log.warn("Unhandled declaration type: ${ctx.javaClass.simpleName}")
                    newProblemDeclaration(
                        "Unhandled declaration type: ${ctx.javaClass.simpleName}",
                        rawNode = ctx,
                    )
                }
            }
        } catch (e: Exception) {
            log.error("Error while handling a declaration", e)
            return newProblemDeclaration(
                "Error handling declaration ${ctx}: ${e.message}",
                rawNode = ctx,
            )
        }
    }

    private fun handleClass(sootClass: SootClass): Record {
        val record =
            newRecord(
                sootClass.getName(),
                if (sootClass.isInterface()) {
                    "interface"
                } else {
                    "class"
                },
                rawNode = sootClass,
            )

        // Collect super class
        val o = sootClass.superclass
        if (o.isPresent) {
            record.addSuperClass(frontend.typeOf(o.get()))
        }

        // Collect implemented interfaces
        for (i in sootClass.interfaces) {
            record.implementedInterfaces += frontend.typeOf(i)
        }

        // Enter the class scope
        frontend.scopeManager.enterScope(record)

        // Loop through all fields
        for (sootField in sootClass.fields) {
            val field = handle(sootField) as? Field
            if (field != null) {
                frontend.scopeManager.addDeclaration(field)
                record.addDeclaration(field)
            }
        }

        // Loop through all methods
        for (sootMethod in sootClass.methods) {
            val method = handle(sootMethod) as? Method
            if (method != null) {
                frontend.scopeManager.addDeclaration(method)
                record.addDeclaration(method)
            }
        }

        // Leave the class scope
        frontend.scopeManager.leaveScope(record)

        return record
    }

    private fun handleMethod(sootMethod: SootMethod): Method {
        val record = frontend.scopeManager.currentRecord

        val method =
            if (sootMethod.name == "<init>") {
                newConstructor(sootMethod.name, record, rawNode = sootMethod)
            } else {
                newMethod(
                    sootMethod.name,
                    sootMethod.isStatic,
                    frontend.scopeManager.currentRecord,
                    rawNode = sootMethod,
                )
            }

        // Enter method scope
        frontend.scopeManager.enterScope(method)

        // Add "@this" as the receiver
        val receiver =
            newVariable("@this", method.recordDeclaration?.toType() ?: unknownType())
                .implicit("@this")
        frontend.scopeManager.addDeclaration(receiver)
        method.receiver = receiver

        // Add method parameters
        for ((index, type) in sootMethod.parameterTypes.withIndex()) {
            val param = newParameter("@parameter${index}", frontend.typeOf(type))
            frontend.scopeManager.addDeclaration(param)
            method.parameters += param
        }

        // Parse body if doNotParseBody returns false
        if (!frontend.frontendConfiguration.doNotParseBody(method) && sootMethod.isConcrete) {
            // Handle method body
            method.body = frontend.statementHandler.handle(sootMethod.body)
        }

        // Leave method scope
        frontend.scopeManager.leaveScope(method)

        return method
    }

    fun handleField(field: SootField): Field {
        return newField(
            field.name,
            frontend.typeOf(field.type),
            field.modifiers.map { it.name.lowercase() }.toSet(),
            rawNode = field,
        )
    }

    private fun handleLocal(local: Local): Variable {
        return newVariable(local.name, frontend.typeOf(local.type), rawNode = local)
    }
}
