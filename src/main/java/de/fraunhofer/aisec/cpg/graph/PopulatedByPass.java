package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.passes.Pass;

/**
 * This annotation denotes that, this property is populates by a pass. Optionally, also specifying
 * which Pass class is responsible.
 */
public @interface PopulatedByPass {
  Class<? extends Pass> value();
}
