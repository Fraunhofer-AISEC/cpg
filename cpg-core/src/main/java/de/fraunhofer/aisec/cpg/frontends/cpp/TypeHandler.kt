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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.types.*
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTReferenceOperator
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName

fun CXXLanguageFrontend.typeOf(declarator: IASTDeclarator, specifier: IASTDeclSpecifier): Type {
    // TODO: in general, we should parse the qualifiers, such as const here, instead of in the
    // TypeParser
    var type =
        when (specifier) {
            is IASTSimpleDeclSpecifier -> {
                // A primitive type
                TypeParser.createFrom(specifier.rawSignature, false)
            }
            is IASTNamedTypeSpecifier -> {
                val name = specifier.name
                val nameString: String =
                    if (name is CPPASTQualifiedName) {
                        // For some reason the legacy type system does not keep the language
                        // specific namespace delimiters, and for backwards compatibility, we are
                        // keeping this behaviour (for now).
                        specifier.rawSignature.replace("::", ".")
                    } else {
                        specifier.rawSignature
                    }

                TypeParser.createFrom(nameString, true, this)
            }
            is IASTCompositeTypeSpecifier -> {
                // A class. This actually also declares the class. At the moment, we handle this in
                // handleSimpleDeclaration, but we might want to move it here
                TypeParser.createFrom(specifier.rawSignature, true, this)
            }
            is IASTElaboratedTypeSpecifier -> {
                // A class or struct
                TypeParser.createFrom(specifier.rawSignature, true, this)
            }
            else -> {
                UnknownType.getUnknownType()
            }
        }

    type = this.adjustType(declarator, type)
    return type
}

fun CXXLanguageFrontend.adjustType(declarator: IASTDeclarator, incoming: Type): Type {
    var type = incoming

    // First, look at the declarator's pointer operator, to see whether, we need to wrap the type
    // into a pointer or similar
    for (op in declarator.pointerOperators) {
        type =
            when (op) {
                is IASTPointer -> {
                    type.reference(PointerType.PointerOrigin.POINTER)
                }
                is ICPPASTReferenceOperator -> {
                    ReferenceType(type.storage, type.qualifier, type)
                }
                else -> {
                    type
                }
            }
    }

    // Check, if we are an array type
    if (declarator is IASTArrayDeclarator) {
        for (mod in declarator.arrayModifiers) {
            type = type.reference(PointerType.PointerOrigin.ARRAY)
        }
    } else if (declarator is IASTStandardFunctionDeclarator) {
        // Loop through the parameters
        val paramTypes = declarator.parameters.map { typeOf(it.declarator, it.declSpecifier) }

        // We need to construct a function (pointer) type here. The existing type
        // so far is the return value. We then add the parameters
        type = FunctionPointerType(type.qualifier, type.storage, paramTypes, type)
    }

    // Lastly, there might be further nested declarators that adjust the type further
    if (declarator.nestedDeclarator != null) {
        type = adjustType(declarator.nestedDeclarator, type)
    }

    // Make sure, the type manager knows about this type
    return TypeManager.getInstance().registerType(type)
}
