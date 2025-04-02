package de.fraunhofer.aisec.cpg.assumptions

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode

class Assumption(assumptionType: AssumptionType,
                 node: Node,
                 scope: Node = node,
                 fileAndLine: String,
                 message: String) : OverlayNode() {

    init {
        super.underlyingNode = scope
    }


}

enum class AssumptionType {
    InferenceAssumption, ClosedMacroAssumption, UnsupportedLanguageProblem, MissingCodeProblem,
    AmbiguityAssumption,
    ConceptPlacementAssumption, ExhaustivEnumerationAssumption,
    CompletenessAssumption, SoundnessAssumption,
    CFIntegrityAssumption, NoExceptionsAssumption, CFAllOrNothingExecutesAssumption, TrustedConfigAssumption, ExternalDataAssumption,
    NetworkAvailableAssumption, ResourceExistsAssumption, ServiceReachableAssumption,
    AtomicExecutionAssumption,
    TrustBoundaryAssumption, DataRangeAssumption, TrustedInputAssumption,
}

