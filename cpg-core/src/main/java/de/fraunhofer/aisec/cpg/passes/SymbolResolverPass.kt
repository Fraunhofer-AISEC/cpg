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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util

abstract class SymbolResolverPass : Pass() {
    protected lateinit var walker: SubgraphWalker.ScopedWalker
    lateinit var currentTU: TranslationUnitDeclaration

    val recordMap = mutableMapOf<String, RecordDeclaration>()
    protected val enumMap = mutableMapOf<Type, EnumDeclaration>()
    protected val templateList = mutableListOf<TemplateDeclaration>()
    protected val superTypesMap = mutableMapOf<String, List<Type>>()

    /** Maps the name of the type of record declarations to its declaration. */
    protected fun findRecords(node: Node) {
        if (node is RecordDeclaration) {
            // The type name is not the same as the node's name! So, we have to be careful when
            // using the map!
            val type = TypeParser.createFrom(node.name, true)
            recordMap.putIfAbsent(type.typeName, node)
        }
    }

    /** Maps the type of enums to its declaration. */
    protected fun findEnums(node: Node) {
        if (node is EnumDeclaration) {
            val type = TypeParser.createFrom(node.name, true)
            enumMap.putIfAbsent(type, node)
        }
    }

    /** Caches all TemplateDeclarations in [templateList] */
    protected fun findTemplates(node: Node) {
        if (node is TemplateDeclaration) {
            templateList.add(node)
        }
    }

    /**
     * Checks if the function has the given [name], and returnType and signature specified in
     * [fctPtrType].
     */
    protected fun FunctionDeclaration.matches(
        name: String,
        fctPtrType: FunctionPointerType
    ): Boolean {
        return this.matches(name, fctPtrType.returnType, fctPtrType.parameters)
    }

    /** Checks if the function has the given [name], [returnType] and [signature] */
    protected fun FunctionDeclaration.matches(
        name: String,
        returnType: Type,
        signature: List<Type?>
    ): Boolean {
        val thisReturnType =
            if (this.returnTypes.isEmpty()) {
                IncompleteType()
            } else {
                // TODO(oxisto): support multiple return types
                this.returnTypes[0]
            }
        return this.name == name && thisReturnType == returnType && this.hasSignature(signature)
    }

    protected fun collectSupertypes() {
        val currSuperTypes = recordMap.mapValues { (_, value) -> value.superTypes }
        superTypesMap.putAll(currSuperTypes)
    }

    fun createInferredFunctionDeclaration(
        containingRecord: RecordDeclaration?,
        name: String?,
        code: String?,
        isStatic: Boolean,
        signature: List<Type?>,
        returnType: Type?
    ): FunctionDeclaration {
        log.debug(
            "Inferring a new method declaration $name with parameter types ${signature.map { it?.name }}"
        )
        if (containingRecord?.isInferred == true && containingRecord.kind == "struct") {
            // "upgrade" our struct to a class, if it was inferred by us, since we are calling
            // methods on it
            containingRecord.kind = "class"
        }

        val parameters = Util.createInferredParameters(signature)
        val declarationHolder: DeclarationHolder = containingRecord ?: currentTU
        val inferred: FunctionDeclaration =
            if (containingRecord != null) {
                NodeBuilder.newMethodDeclaration(name, code, isStatic, containingRecord)
            } else {
                NodeBuilder.newFunctionDeclaration(name!!, code)
            }
        inferred.isInferred = true
        inferred.parameters = parameters
        // TODO: Once, we used inferred.type = returnType and once the two following statements:
        // Why? What's the "right way"?
        returnType?.let { inferred.returnTypes = listOf(it) }
        inferred.type = returnType
        // TODO: Handle multiple return values?
        if (declarationHolder is RecordDeclaration) {
            declarationHolder.addMethod(inferred as MethodDeclaration)
            if (isStatic) {
                declarationHolder.staticImports.add(inferred)
            }
        } else {
            declarationHolder.addDeclaration(inferred)
        }

        return inferred
    }

    /**
     * Infers a record declaration for the given type. [type] is the object type representing a
     * record that we want to infer, the [recordToUpdate] is either the type's name or the type's
     * root name. The [kind] specifies if we create a class or a struct.
     */
    protected fun inferRecordDeclaration(
        type: Type,
        recordToUpdate: String,
        kind: String = "class"
    ): RecordDeclaration? {
        if (type !is ObjectType) {
            log.error(
                "Trying to infer a record declaration of a non-object type. Not sure what to do? Should we change the type?"
            )
            return null
        }
        log.debug(
            "Encountered an unknown record type ${type.typeName} during a call. We are going to infer that record"
        )

        // This could be a class or a struct. We start with a class and may have to fine-tune this
        // later.
        // TODO: used to be a struct in the VariableUsageResolver and a class in the
        // CallResolver. Both said that the kind could have been wrong and should be updated
        // later. However, I don't know where/if this ever happened.
        val declaration = NodeBuilder.newRecordDeclaration(type.typeName, kind, "")
        declaration.isInferred = true

        // update the type
        type.recordDeclaration = declaration

        // update the record map
        recordMap[recordToUpdate] = declaration

        // add this record declaration to the current TU (this bypasses the scope manager)
        currentTU.addDeclaration(declaration)
        return declaration
    }

    override fun cleanup() {
        superTypesMap.clear()
        recordMap.clear()
        enumMap.clear()
        templateList.clear()
    }

    val Node.delimiter: String
        get() = lang!!.namespaceDelimiter

    val Node.language: LanguageFrontend
        get() = lang!!
}
