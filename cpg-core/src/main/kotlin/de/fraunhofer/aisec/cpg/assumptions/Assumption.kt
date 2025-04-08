package de.fraunhofer.aisec.cpg.assumptions

import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode

class Assumption(assumptionType: AssumptionType,
                 node: Node,
                 scope: Node = node,
                 assumptionLocation: String, // If the assumption was made the CPG sourcecode, the cpg sourcecode locations is used
                 message: String) : OverlayNode() {

    init {
        super.underlyingNode = scope
        name = Name(assumptionType.name)
        location = node.location
    }

    companion object {
        /**
         *
         * @param assumptionType The type of assumption used to differentiate between assumptions and group similar assumptions.
         *
         * @param node The node that cause the assumption to be necessary, even if the assumption has validity for the
         * entire program, the node serves as location to be reported to the user.
         *
         * @param scope The scope that the assumption has validity for, here the scope is a node, because the assumption is
         * valid for every node in its ast subtree.
         *
         * @param message The message describing the assumption that was taken.
         */
        public fun assume(
            assumptionType: AssumptionType,
            node: Node,
            scope: Node = node,
            message: () -> String,
        ) {
            // This connects the assumption as an overlay node to the code graph
            Assumption(assumptionType, node, scope, getCurrentFileAndLine(), message.toString())
        }


        private fun getCurrentFileAndLine(): String {
            val stackTrace = Thread.currentThread().stackTrace
            return "File: ${stackTrace[3].fileName}, Line: ${stackTrace[3].lineNumber}"
        }
    }

}

enum class AssumptionType {
    InferenceAssumption,
    ClosedMacroAssumption,
    UnsupportedLanguageProblem,
    MissingCodeProblem,
    AmbiguityAssumption,
    ConceptPlacementAssumption,
    ExhaustiveEnumerationAssumption,
    CompletenessAssumption,
    SoundnessAssumption,
    CFIntegrityAssumption,
    NoExceptionsAssumption,
    CFAllOrNothingExecutesAssumption,
    TrustedConfigAssumption,
    ExternalDataAssumption,
    NetworkAvailableAssumption,
    ResourceExistsAssumption,
    ServiceReachableAssumption,
    AtomicExecutionAssumption,
    TrustBoundaryAssumption,
    DataRangeAssumption,
    TrustedInputAssumption,
}

