package de.fraunhofer.aisec.cpg.graph.edge;

/**
 * INDEX:(int) Indicates the position in a list of edges
 *
 * <p>BRANCH:(boolean) If we have multiple EOG edges the branch property indicates which EOG edge
 * leads to true branch (expression evaluated to true) or the false branch (e.g. with an if/else
 * contidion)
 *
 * <p>DEFAULT:(boolean) Indicates which arguments edge of a CallExpression leads to a default
 * argument
 */
public enum Properties {
  INDEX,
  BRANCH,
  DEFAULT,
}
