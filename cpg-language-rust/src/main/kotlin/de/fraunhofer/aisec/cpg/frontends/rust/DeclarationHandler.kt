/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import uniffi.cpgrust.RsAst
import uniffi.cpgrust.RsFn
import uniffi.cpgrust.RsItem
import uniffi.cpgrust.RsParam
import uniffi.cpgrust.RsPat

class DeclarationHandler(frontend: RustLanguageFrontend) :
    RustHandler<Declaration, RsAst.RustItem>(::ProblemDeclaration, frontend) {
    override fun handleNode(node: RsAst.RustItem): Declaration {
        val item = node.v1
        return when (item) {
            is RsItem.Fn -> handleFunctionDeclaration(item.v1)
            is RsItem.Param -> handleParameterDeclaration(item.v1)
            else -> handleNotSupported(node, node::class.simpleName ?: "")
        }
    }

    private fun handleFunctionDeclaration(fn: RsFn): FunctionDeclaration {
        val function =
            newFunctionDeclaration(fn.name ?: "", rawNode = RsAst.RustItem(RsItem.Fn(fn)))
        frontend.scopeManager.enterScope(function)
        for (param in fn.paramList?.params ?: listOf()) {
            function.parameters += handleParameterDeclaration(param) as ParameterDeclaration
        }

        fn.body?.let { function.body = frontend.expressionHandler.handleBlockExpr(it) }

        frontend.scopeManager.leaveScope(function)
        return function
    }

    private fun handleParameterDeclaration(param: RsParam): Declaration {

        val type = param.ty?.let { frontend.typeOf(it) }

        val name = (param.pat as? RsPat.IdentPat)?.v1?.name ?: ""

        val parameter =
            newParameterDeclaration(
                name,
                type = type ?: unknownType(),
                rawNode = RsAst.RustItem(RsItem.Param(param)),
            )

        return parameter
    }
}
