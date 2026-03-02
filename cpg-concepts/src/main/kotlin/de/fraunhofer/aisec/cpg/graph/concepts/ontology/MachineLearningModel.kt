package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList
import kotlin.collections.MutableMap

public open class MachineLearningModel(
  public val adversarialRobustnessScore: Float?,
  public val evasionEfficacyLevel: Float?,
  public val explainability: Float?,
  public val explainabilityEnabled: Boolean?,
  public val membershipInferenceResilience: Float?,
  public val modelStealResilience: Float?,
  public val poisonedDataLevel: Float?,
  public val poisoningResilienceLevel: Float?,
  public val vulnerabilities: MutableList<Vulnerability?>,
  dataLocation: DataLocation?,
  creation_time: ZonedDateTime?,
  description: String?,
  resourceId: String?,
  labels: MutableMap<String, String>?,
  name: String?,
  raw: String?,
  parent: Resource?,
  underlyingNode: Node? = null,
) : MachineLearning(dataLocation, creation_time, description, resourceId, labels, name, raw, parent, underlyingNode) {
  init {
    name?.let { this.name = Name(localName = it) }
  }

  override fun equals(other: Any?): Boolean = other is MachineLearningModel &&
              super.equals(other) &&
              other.adversarialRobustnessScore == this.adversarialRobustnessScore &&
              other.evasionEfficacyLevel == this.evasionEfficacyLevel &&
              other.explainability == this.explainability &&
              other.explainabilityEnabled == this.explainabilityEnabled &&
              other.membershipInferenceResilience == this.membershipInferenceResilience &&
              other.modelStealResilience == this.modelStealResilience &&
              other.poisonedDataLevel == this.poisonedDataLevel &&
              other.poisoningResilienceLevel == this.poisoningResilienceLevel &&
              other.vulnerabilities == this.vulnerabilities

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              adversarialRobustnessScore,
              evasionEfficacyLevel,
              explainability,
              explainabilityEnabled,
              membershipInferenceResilience,
              modelStealResilience,
              poisonedDataLevel,
              poisoningResilienceLevel,
              vulnerabilities,
          )
}
