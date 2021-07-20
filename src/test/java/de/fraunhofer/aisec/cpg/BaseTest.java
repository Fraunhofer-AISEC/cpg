package de.fraunhofer.aisec.cpg;

import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

  /**
   * {@link TypeParser} and {@link TypeManager} hold static state. This needs to be cleared before
   * all tests in order to avoid strange errors
   */
  @BeforeEach
  protected void resetPersistentState() {
    TypeParser.reset();
    TypeManager.reset();
  }
}
