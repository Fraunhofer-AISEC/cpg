package de.fraunhofer.aisec.cpg.frontends.cpp;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.Expression;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.io.File;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CXXBindingsTest {

  /**
   * {@link TypeParser} and {@link TypeManager} hold static state. This needs to be cleared before
   * all tests in order to avoid strange errors
   */
  @BeforeEach
  void resetPersistentState() {
    TypeParser.reset();
    TypeManager.reset();
  }

  void checkBindings(CXXLanguageFrontend cxxLanguageFrontend) {
    for (IBinding binding : cxxLanguageFrontend.getCachedDeclarations().keySet()) {
      Declaration declaration = cxxLanguageFrontend.getCachedDeclaration(binding);
      for (Expression expression : cxxLanguageFrontend.getCachedExpression(binding)) {
        for (Declaration refersTo : ((DeclaredReferenceExpression) expression).getRefersTo()) {
          assertEquals(declaration, refersTo);
        }
      }
    }
  }

  @Test
  void testUseThenDeclaration() throws Exception {
    CXXLanguageFrontend cxxLanguageFrontend =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build(), new ScopeManager());
    cxxLanguageFrontend.parse(new File("src/test/resources/bindings/use_then_declare.cpp"));

    checkBindings(cxxLanguageFrontend);
  }

  @Test
  void testDeclarationReplacement() throws Exception {
    CXXLanguageFrontend cxxLanguageFrontend =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build(), new ScopeManager());
    cxxLanguageFrontend.parse(new File("src/test/resources/bindings/replace_declaration.cpp"));

    checkBindings(cxxLanguageFrontend);
  }
}
