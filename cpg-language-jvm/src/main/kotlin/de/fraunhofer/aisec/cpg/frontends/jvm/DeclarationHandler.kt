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
import sootup.java.core.JavaSootClass
import sootup.java.core.JavaSootField
import sootup.java.core.JavaSootMethod
import sootup.java.core.jimple.basic.JavaLocal

class DeclarationHandler(frontend: JVMLanguageFrontend) :
    Handler<Declaration, Any, JVMLanguageFrontend>(::ProblemDeclaration, frontend) {
    init {
        map.put(SootClass::class.java) { handleClass(it as SootClass) }
        map.put(JavaSootClass::class.java) { handleClass(it as SootClass) }
        map.put(SootMethod::class.java) { handleMethod(it as SootMethod) }
        map.put(JavaSootMethod::class.java) { handleMethod(it as SootMethod) }
        map.put(SootField::class.java) { handleField(it as SootField) }
        map.put(JavaSootField::class.java) { handleField(it as SootField) }
        map.put(Local::class.java) { handleLocal(it as Local) }
        map.put(JavaLocal::class.java) { handleLocal(it as Local) }
    }

    private fun handleClass(sootClass: SootClass): RecordDeclaration {
        val record =
            newRecordDeclaration(
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
        enterScope(record)

        // Loop through all fields
        for (sootField in sootClass.fields) {
            val field = handle(sootField) as? FieldDeclaration
            if (field != null) {
                declareSymbol(field)
                record.addDeclaration(field)
            }
        }

        // Loop through all methods
        for (sootMethod in sootClass.methods) {
            val method = handle(sootMethod) as? MethodDeclaration
            if (method != null) {
                declareSymbol(method)
                record.addDeclaration(method)
            }
        }

        // Leave the class scope
        leaveScope(record)

        return record
    }

    private fun handleMethod(sootMethod: SootMethod): MethodDeclaration {
        val record = currentRecord

        val method =
            if (sootMethod.name == "<init>") {
                newConstructorDeclaration(sootMethod.name, record, rawNode = sootMethod)
            } else {
                newMethodDeclaration(
                    sootMethod.name,
                    sootMethod.isStatic,
                    currentRecord,
                    rawNode = sootMethod,
                )
            }

        // Enter method scope
        enterScope(method)

        // Add "@this" as the receiver
        val receiver =
            newVariableDeclaration("@this", method.recordDeclaration?.toType() ?: unknownType())
                .implicit("@this")
        declareSymbol(receiver)
        method.receiver = receiver

        // Add method parameters
        for ((index, type) in sootMethod.parameterTypes.withIndex()) {
            val param = newParameterDeclaration("@parameter${index}", frontend.typeOf(type))
            declareSymbol(param)
            method.parameters += param
        }

        if (sootMethod.isConcrete) {
            // Handle method body
            method.body = frontend.statementHandler.handle(sootMethod.body)
        }

        // Leave method scope
        leaveScope(method)

        return method
    }

    fun handleField(field: SootField): FieldDeclaration {
        return newFieldDeclaration(
            field.name,
            frontend.typeOf(field.type),
            field.modifiers.map { it.name.lowercase() },
            rawNode = field,
        )
    }

    private fun handleLocal(local: Local): VariableDeclaration {
        return newVariableDeclaration(local.name, frontend.typeOf(local.type), rawNode = local)
    }
}
