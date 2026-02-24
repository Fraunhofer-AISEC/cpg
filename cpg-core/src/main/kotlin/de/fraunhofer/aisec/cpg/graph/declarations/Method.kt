/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.persistence.Relationship

/** A method declaration is a [Function] that is part of to a specific [Record] . */
open class Method : Function() {
    /** The [Record] this method is part of. This can be empty if we do not know about it. */
    open var recordDeclaration: Record? = null

    @Relationship("RECEIVER") var receiverEdge = astOptionalEdgeOf<Variable>()
    /**
     * The receiver variable of this method. In most cases, this variable is called `this`, but in
     * some languages, it is `self` (e.g. in Rust or Python) or can be freely named (e.g. in
     * Golang). It can be empty, i.e., for pure function declarations as part of an interface.
     *
     * Hints for language frontend developers: When a method is translated, the receiver must be
     * created (usually in the form of a [Variable] and added to the scope of the [Method] by a
     * language frontend. Furthermore, it must be manually set to the [receiver] property of the
     * method, since the scope manager cannot do this. If the name of the receiver, e.g., `this`, is
     * used anywhere in the method body, a [Reference] must be created by the language frontend, and
     * its [Reference.refersTo] property must point to this [receiver]. The latter is done
     * automatically by the [SymbolResolver], which treats it like any other regular variable.
     *
     * Some languages (for example Python) denote the first argument in a method declaration as the
     * receiver (e.g., in `def foo(self, arg1)`, `self` is the receiver). In this case, extra care
     * needs to be taken that for the first argument of the method, a [Variable] is created and
     * stored in [receiver]. All other arguments must then be processed normally (usually into a
     * [Parameter]). This is also important because from the "outside" the method only has the
     * remaining arguments, when called (e.g., `object.foo("myarg1")`).
     *
     * There is one special case that concerns the Java language: In Java, there also exists a
     * `super` keyword, which can be used to explicitly access methods or fields of the (single)
     * superclass of the current class. In this case, a [Reference] will also be created (with the
     * name `super`) and it will also refer to this receiver, even though the receiver's name is
     * `this`. This is one of the very few exceptions where the reference and its declaration do not
     * share the same name. The [SymbolResolver] will recognize this and treat the scoping aspect of
     * the super-call accordingly.
     */
    var receiver by unwrapping(Method::receiverEdge)
}
