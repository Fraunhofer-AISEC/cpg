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

import uniffi.cpgrust.RsAst
import uniffi.cpgrust.RsExpr
import uniffi.cpgrust.RsItem
import uniffi.cpgrust.RsNode
import uniffi.cpgrust.RsStmt

interface Rust {
    interface Type {}
}

/**
 * I dislike accessing a field by continuous extending of an access function, but Rust does not
 * support inheritance, and therefore the generated bindings don't either. We cannot specify that a
 * field exists in several class.
 */
fun RsAst.astNode(): RsNode {
    return when (this) {
        is RsAst.RustExpr -> this.v1.astNode()
        is RsAst.RustItem -> this.v1.astNode()
        is RsAst.RustStmt -> this.v1.astNode()
    }
}

fun RsExpr.astNode(): RsNode {
    return when (this) {
        is RsExpr.ArrayExpr -> this.v1.astNode
        is RsExpr.Literal -> this.v1.astNode
        is RsExpr.AsmExpr -> this.v1.astNode
        is RsExpr.IfExpr -> this.v1.astNode
        is RsExpr.ParenExpr -> this.v1.astNode
        is RsExpr.AwaitExpr -> this.v1.astNode
        is RsExpr.BecomeExpr -> this.v1.astNode
        is RsExpr.BinExpr -> this.v1.astNode
        is RsExpr.BlockExpr -> this.v1.astNode
        is RsExpr.BreakExpr -> this.v1.astNode
        is RsExpr.CallExpr -> this.v1.astNode
        is RsExpr.CastExpr -> this.v1.astNode
        is RsExpr.ClosureExpr -> this.v1.astNode
        is RsExpr.ContinueExpr -> this.v1.astNode
        is RsExpr.FieldExpr -> this.v1.astNode
        is RsExpr.ForExpr -> this.v1.astNode
        is RsExpr.FormatArgsExpr -> this.v1.astNode
        is RsExpr.IndexExpr -> this.v1.astNode
        is RsExpr.LetExpr -> this.v1.astNode
        is RsExpr.LoopExpr -> this.v1.astNode
        is RsExpr.MacroExpr -> this.v1.astNode
        is RsExpr.MatchExpr -> this.v1.astNode
        is RsExpr.MethodCallExpr -> this.v1.astNode
        is RsExpr.OffsetOfExpr -> this.v1.astNode
        is RsExpr.PathExpr -> this.v1.astNode
        is RsExpr.PrefixExpr -> this.v1.astNode
        is RsExpr.RangeExpr -> this.v1.astNode
        is RsExpr.RecordExpr -> this.v1.astNode
        is RsExpr.RefExpr -> this.v1.astNode
        is RsExpr.ReturnExpr -> this.v1.astNode
        is RsExpr.TryExpr -> this.v1.astNode
        is RsExpr.TupleExpr -> this.v1.astNode
        is RsExpr.UnderscoreExpr -> this.v1.astNode
        is RsExpr.WhileExpr -> this.v1.astNode
        is RsExpr.YeetExpr -> this.v1.astNode
        is RsExpr.YieldExpr -> this.v1.astNode
    }
}

fun RsItem.astNode(): RsNode {
    return when (this) {
        is RsItem.Fn -> this.v1.astNode
        is RsItem.Module -> this.v1.astNode
        is RsItem.Use -> this.v1.astNode
        is RsItem.Enum -> this.v1.astNode
        is RsItem.Impl -> this.v1.astNode
        is RsItem.AsmExpr -> this.v1.astNode
        is RsItem.Const -> this.v1.astNode
        is RsItem.ExternBlock -> this.v1.astNode
        is RsItem.ExternCrate -> this.v1.astNode
        is RsItem.MacroCall -> this.v1.astNode
        is RsItem.MacroDef -> this.v1.astNode
        is RsItem.MacroRules -> this.v1.astNode
        is RsItem.Static -> this.v1.astNode
        is RsItem.Struct -> this.v1.astNode
        is RsItem.Trait -> this.v1.astNode
        is RsItem.TypeAlias -> this.v1.astNode
        is RsItem.Union -> this.v1.astNode
    }
}

fun RsStmt.astNode(): RsNode {
    return when (this) {
        is RsStmt.ExprStmt -> this.v1.astNode
        is RsStmt.LetStmt -> this.v1.astNode
        is RsStmt.Item -> this.v1.astNode()
    }
}
