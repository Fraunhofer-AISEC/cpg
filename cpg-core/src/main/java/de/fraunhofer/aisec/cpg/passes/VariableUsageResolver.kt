/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFieldDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

/**
 * Creates new connections between the place where a variable is declared and where it is used.
 *
 * A field access is modeled with a [MemberExpression]. After AST building, its base and member
 * references are set to [DeclaredReferenceExpression] stubs. This pass resolves those references
 * and makes the member point to the appropriate [FieldDeclaration] and the base to the "this"
 * [FieldDeclaration] of the containing class. It is also capable of resolving references to fields
 * that are inherited from a superclass and thus not declared in the actual base class. When base or
 * member declarations are not found in the graph, a new "inferred" [FieldDeclaration] is being
 * created that is then used to collect all usages to the same unknown declaration.
 * [DeclaredReferenceExpression] stubs are removed from the graph after being resolved.
 *
 * Accessing a local variable is modeled directly with a [DeclaredReferenceExpression]. This step of
 * the pass doesn't remove the [DeclaredReferenceExpression] nodes like in the field usage case but
 * rather makes their "refersTo" point to the appropriate [ValueDeclaration].
 */
@DependsOn(TypeHierarchyResolver::class)
open class VariableUsageResolver : SymbolResolverPass() {

    override fun accept(result: TranslationResult) {
        if (lang == null) {
            log.error("No language frontend specified. Can't resolve anything.")
            return
        }

        scopeManager = lang!!.scopeManager
        config = lang!!.config

        walker = ScopedWalker(scopeManager)
        for (tu in result.translationUnits) {
            currentTU = tu
            walker.clearCallbacks()
            walker.registerHandler { _, _, currNode -> walker.collectDeclarations(currNode) }
            walker.registerHandler { node, _ -> findRecords(node) }
            walker.registerHandler { node, _ -> findEnums(node) }
            walker.iterate(currentTU)
        }

        collectSupertypes()

        for (tu in result.translationUnits) {
            walker.clearCallbacks()
            walker.registerHandler { curClass, _, node -> resolveFieldUsages(curClass, node) }
            walker.iterate(tu)
        }
        for (tu in result.translationUnits) {
            walker.clearCallbacks()
            walker.registerHandler(::resolveLocalVarUsage)
            walker.iterate(tu)
        }
    }

    protected fun resolveFunctionPtr(
        containingClassArg: Type?,
        reference: DeclaredReferenceExpression
    ): ValueDeclaration? {
        var containingClass = containingClassArg
        // Without FunctionPointerType, we cannot resolve function pointers
        val fptrType = reference.type as? FunctionPointerType ?: return null

        var functionName = reference.name
        val matcher =
            Pattern.compile("(?:(?<class>.*)(?:\\.|::))?(?<function>.*)").matcher(reference.name)
        if (matcher.matches()) {
            val cls = matcher.group("class")
            functionName = matcher.group("function")
            if (cls == null) {
                log.error(
                    "Resolution of pointers to functions inside the current scope should have been done by the ScopeManager"
                )
            } else {
                containingClass = TypeParser.createFrom(cls, true)
                if (containingClass.typeName in recordMap) {
                    val target =
                        recordMap[containingClass.typeName]!!.methods.firstOrNull { f ->
                            // TODO(oxisto): there is the same logic in the CallResolver.. why
                            f.matches(functionName, fptrType)
                        }
                    if (target != null) return target
                }
            }
        }

        return if (containingClass == null) {
            handleUnknownMethod(functionName, fptrType.returnType, fptrType.parameters)
        } else {
            handleUnknownClassMethod(
                containingClass,
                functionName,
                fptrType.returnType,
                fptrType.parameters
            )
        }
    }

    protected fun resolveLocalVarUsage(
        currentClass: RecordDeclaration?,
        parent: Node?,
        current: Node
    ) {
        if (current !is DeclaredReferenceExpression || current is MemberExpression) return
        if (
            parent is MemberCallExpression &&
                current === parent.member &&
                current.type !is FunctionPointerType
        ) {
            // members of a MemberCallExpression are no variables to be resolved, unless we have
            // a function pointer call
            return
        }

        // For now, we need to ignore reference expressions that are directly embedded into call
        // expressions, because they are the "callee" property. In the future, we will use this
        // property to actually resolve the function call.
        if (parent is CallExpression && parent.callee === current) {
            return
        }

        // only consider resolving, if the language frontend did not specify a resolution
        var refersTo = current.refersTo ?: scopeManager?.resolveReference(current)
        // if (current.refersTo == null) scopeManager?.resolveReference(current)
        // else current.refersTo!!
        var recordDeclType: Type? = null
        if (currentClass != null) {
            recordDeclType = TypeParser.createFrom(currentClass.name, true)
        }
        if (current.type is FunctionPointerType && refersTo == null) {
            refersTo = resolveFunctionPtr(recordDeclType, current)
        }

        // only add new nodes for non-static unknown
        if (
            refersTo == null &&
                !current.isStaticAccess &&
                recordDeclType != null &&
                recordDeclType.typeName in recordMap
        ) {
            // Maybe we are referring to a field instead of a local var
            if (current.delimiter in current.name) {
                recordDeclType = getEnclosingTypeOf(current)
            }
            val field = resolveMember(recordDeclType, current)
            if (field != null) {
                refersTo = field
            }
        }

        // TODO: we need to do proper scoping (and merge it with the code above), but for now
        // this just enables CXX static fields
        if (refersTo == null && current.delimiter in current.name) {
            recordDeclType = getEnclosingTypeOf(current)
            val field = resolveMember(recordDeclType, current)
            if (field != null) {
                refersTo = field
            }
        }
        if (refersTo != null) {
            current.refersTo = refersTo
        } else {
            Util.warnWithFileLocation(
                current,
                log,
                "Did not find a declaration for ${current.name}"
            )
        }
    }

    /**
     * We get the type of the "scope" this node is in. (e.g. for a field, we drop the field's name
     * and have the class)
     */
    protected fun getEnclosingTypeOf(current: Node): Type {
        val path =
            listOf(
                *current.name
                    .split(Pattern.quote(current.delimiter).toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            )
        return TypeParser.createFrom(
            java.lang.String.join(current.delimiter, path.subList(0, path.size - 1)),
            true
        )
    }

    protected fun resolveFieldUsages(curClass: RecordDeclaration?, current: Node) {
        if (current !is MemberExpression) return

        var baseTarget: Declaration? = null
        if (current.base is DeclaredReferenceExpression) {
            val base = current.base as DeclaredReferenceExpression
            if (current.language is JavaLanguageFrontend && base.name == "super") {
                if (curClass != null && curClass.superClasses.isNotEmpty()) {
                    val superType = curClass.superClasses[0]
                    val superRecord = recordMap[superType.typeName]
                    if (superRecord == null) {
                        log.error(
                            "Could not find referring super type ${superType.typeName} for ${curClass.name} in the record map. Will set the super type to java.lang.Object"
                        )
                        base.type = TypeParser.createFrom(Any::class.java.name, true)
                    } else {
                        // We need to connect this super reference to the receiver of this
                        // method
                        val func = scopeManager?.currentFunction
                        if (func is MethodDeclaration) {
                            baseTarget = func.receiver
                        }
                        if (baseTarget != null) {
                            base.refersTo = baseTarget
                            // Explicitly set the type of the call's base to the super type
                            base.type = superType
                            // And set the possible subtypes, to ensure, that really only our
                            // super type is in there
                            base.updatePossibleSubtypes(listOf(superType))
                        }
                    }
                } else {
                    // no explicit super type -> java.lang.Object
                    val objectType = TypeParser.createFrom(Any::class.java.name, true)
                    base.type = objectType
                }
            } else {
                baseTarget = resolveBase(current.base as DeclaredReferenceExpression)
                base.refersTo = baseTarget
            }
            if (baseTarget is EnumDeclaration) {
                val name = current.name
                val memberTarget = baseTarget.entries.firstOrNull { it.name == name }
                if (memberTarget != null) {
                    current.refersTo = memberTarget
                    return
                }
            } else if (baseTarget is RecordDeclaration) {
                var baseType = TypeParser.createFrom(baseTarget.name, true)
                if (baseType.typeName !in recordMap) {
                    val containingT = baseType
                    val fqnResolvedType =
                        recordMap.keys.firstOrNull {
                            it.endsWith("." + containingT.name)
                        } // TODO: Is the "." correct here for all languages?
                    if (fqnResolvedType != null) {
                        baseType = TypeParser.createFrom(fqnResolvedType, true)
                    }
                }
                current.refersTo = resolveMember(baseType, current)
                return
            }
        }
        var baseType = current.base.type
        if (baseType.typeName !in recordMap) {
            val fqnResolvedType = recordMap.keys.firstOrNull { it.endsWith("." + baseType.name) }
            if (fqnResolvedType != null) {
                baseType = TypeParser.createFrom(fqnResolvedType, true)
            }
        }
        current.refersTo = resolveMember(baseType, current)
    }

    protected fun resolveBase(reference: DeclaredReferenceExpression): Declaration? {
        val declaration = scopeManager?.resolveReference(reference)
        if (declaration != null) {
            return declaration
        }

        // check if this refers to an enum
        return if (reference.type in enumMap) {
            enumMap[reference.type]
        } else if (reference.type.typeName in recordMap) {
            recordMap[reference.type.typeName]
        } else {
            null
        }
    }

    protected fun resolveMember(
        containingClass: Type,
        reference: DeclaredReferenceExpression
    ): ValueDeclaration? {
        if (
            reference.language is JavaLanguageFrontend &&
                reference.name.matches(Regex("(?<class>.+\\.)?super"))
        ) {
            // if we have a "super" on the member side, this is a member call. We need to resolve
            // this in the call resolver instead
            return null
        }
        val simpleName = Util.getSimpleName(reference.delimiter, reference.name)
        var member: FieldDeclaration? = null
        if (containingClass !is UnknownType && containingClass.typeName in recordMap) {
            member =
                recordMap[containingClass.typeName]!!
                    .fields
                    .filter { it.name == simpleName }
                    .map { it.definition }
                    .firstOrNull()
        }
        if (member == null) {
            member =
                superTypesMap
                    .getOrDefault(containingClass.typeName, listOf())
                    .mapNotNull { recordMap[it.typeName] }
                    .flatMap { it.fields }
                    .filter { it.name == simpleName }
                    .map { it.definition }
                    .firstOrNull()
        }
        // Attention: using orElse instead of orElseGet will always invoke unknown declaration
        // handling!
        return member ?: handleUnknownField(containingClass, reference.name, reference.type)
    }

    protected fun handleUnknownField(base: Type, name: String, type: Type): FieldDeclaration? {
        // unwrap a potential pointer-type
        if (base is PointerType) {
            return handleUnknownField(base.elementType, name, type)
        }
        if (base.typeName !in recordMap) {
            if (config?.inferenceConfiguration?.inferRecords == true) {
                // we access an unknown field of an unknown record. so we need to handle that
                val inferredRecord = inferRecordDeclaration(base, base.typeName, "struct")
                if (inferredRecord == null) {
                    log.error(
                        "Can not add inferred field declaration because the record type could not be inferred."
                    )
                    return null
                }
            } else {
                return null
            }
        }

        val recordDeclaration = recordMap[base.typeName]
        if (recordDeclaration == null) {
            log.error("This should not happen. Inferred record is not in the record map.")
            return null
        }
        val declarations = recordDeclaration.fields
        val target = declarations.firstOrNull { it.name == name }

        return if (target == null) {
            val declaration =
                newFieldDeclaration(name, type, listOf<String>(), "", null, null, false)
            recordDeclaration.addField(declaration)
            declaration.isInferred = true
            declaration
        } else {
            target
        }
    }

    protected fun handleUnknownClassMethod(
        base: Type,
        name: String,
        returnType: Type,
        signature: List<Type?>
    ): MethodDeclaration? {
        val containingRecord = recordMap[base.typeName] ?: return null
        val target =
            containingRecord.methods.firstOrNull { f -> f.matches(name, returnType, signature) }
        return if (target == null) {
            val declaration = newMethodDeclaration(name, "", false, containingRecord)
            declaration.type = returnType
            declaration.parameters = Util.createInferredParameters(signature)
            containingRecord.addMethod(declaration)
            declaration.isInferred = true
            declaration
        } else {
            target
        }
    }

    protected fun handleUnknownMethod(
        name: String,
        returnType: Type,
        signature: List<Type?>
    ): FunctionDeclaration {
        // TODO(oxisto): This is actually the fourth place where we resolve function pointers :(
        val target = currentTU.functions.firstOrNull { f -> f.matches(name, returnType, signature) }
        return if (target == null) {
            val declaration = newFunctionDeclaration(name, "")
            declaration.parameters = Util.createInferredParameters(signature)
            declaration.returnTypes = listOf(returnType)
            declaration.type = computeType(declaration)
            currentTU.addDeclaration(declaration)
            declaration.isInferred = true
            declaration
        } else {
            target
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(VariableUsageResolver::class.java)
    }
}
