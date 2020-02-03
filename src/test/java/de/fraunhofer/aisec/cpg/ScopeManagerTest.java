package de.fraunhofer.aisec.cpg;

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ScopeManagerTest {

  private TranslationConfiguration config;

  @BeforeEach
  void setUp() {
    config = TranslationConfiguration.builder().defaultPasses().build();
  }

  @Test
  void testSetScope() throws TranslationException {
    LanguageFrontend frontend = new JavaLanguageFrontend(config, new ScopeManager());

    assert (frontend == frontend.getScopeManager().getLang());

    frontend.setScopeManager(new ScopeManager());
    assert (frontend == frontend.getScopeManager().getLang());
  }
}
