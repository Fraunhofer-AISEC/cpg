package de.fraunhofer.aisec.cpg.frontends.cpp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.Expression;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.io.File;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.junit.jupiter.api.Test;

class CXXBindingsTest extends BaseTest {

  void checkBindings(CXXLanguageFrontend cxxLanguageFrontend) {
    for (IBinding binding : cxxLanguageFrontend.getCachedDeclarations().keySet()) {
      Declaration declaration = cxxLanguageFrontend.getCachedDeclaration(binding);
      for (Expression expression : cxxLanguageFrontend.getCachedExpression(binding)) {
        assertEquals(declaration, ((DeclaredReferenceExpression) expression).getRefersTo());
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
