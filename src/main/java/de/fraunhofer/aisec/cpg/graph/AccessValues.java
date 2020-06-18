package de.fraunhofer.aisec.cpg.graph;

/** Expressions can have multiple types of accesses. Determines dataflow (DFG) edges */
public enum AccessValues {
  READ,
  WRITE,
  READWRITE
}
