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
package workbench

import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Secret
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.OperatorCall
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass
import de.fraunhofer.aisec.cpg.query.and

include { Tagging from "tagging.codyze.kts" }

/**
 * A small WiFi application to try the features of the console: calls that cannot be resolved or
 * leave the analysed code, branches and loops, a key that flows into encryption, the network and
 * (in debug mode) the log, and concepts attached to some of the calls.
 */
project {
    name = "Workbench Demo"

    tool {
        configuration {
            loadIncludes(true)
            // The dependence graph is shown as a slice of the code in the console
            registerPass<ControlDependenceGraphPass>()
            registerPass<ProgramDependenceGraphPass>()
        }
    }

    toe {
        name = "WiFi Demo"
        architecture {
            modules {
                module("wifi_demo") {
                    directory = "components/wifi_demo"
                    includeAll()
                }
            }
        }
    }

    requirements {
        category("KEYS") {
            requirement {
                name = "Keys are encrypted"
                description = "A key from get_key is passed to encrypt on some path"

                fulfilledBy { keysAreEncrypted() }
            }

            requirement {
                name = "All calls are resolved"
                description = "The analysis knows the target of every call (this fails on purpose)"

                fulfilledBy { allCallsResolved() }
            }
        }
    }

    assumptions { assume { "The code behind the external calls behaves as documented." } }
}

context(tr: TranslationResult)
fun keysAreEncrypted(): QueryTree<Boolean> {
    return tr.allExtended<Secret> { dataFlow(it, predicate = { it.name.localName == "encrypt" }) }
}

context(tr: TranslationResult)
fun allCallsResolved(): QueryTree<Boolean> {
    return tr.allExtended<Call>(sel = { it !is OperatorCall }) { it.invokes.size gt 0 }
}
