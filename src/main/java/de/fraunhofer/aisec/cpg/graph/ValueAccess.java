package de.fraunhofer.aisec.cpg.graph;

/** Expressions can have multiple types of accesses. Determines dataflow (DFG) edges */
public interface ValueAccess {
  enum accessValues {
    READ,
    WRITE,
    READWRITE
  }

  void setAccess(accessValues access);
}
