package de.fraunhofer.aisec.cpg.frontends.cpp;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.Expression;
import de.fraunhofer.aisec.cpg.passes.scopes.GlobalScope;
import java.io.File;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.junit.jupiter.api.Test;

class CXXBindingsTest extends BaseTest {

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
    File file = new File("src/test/resources/bindings/use_then_declare.cpp");
    CXXLanguageFrontend cxxLanguageFrontend =
        new CXXLanguageFrontend(
            file, TranslationConfiguration.builder().build(), new GlobalScope());
    cxxLanguageFrontend.parse(file);

    checkBindings(cxxLanguageFrontend);
  }

  @Test
  void testDeclarationReplacement() throws Exception {
    File file = new File("src/test/resources/bindings/replace_declaration.cpp");
    CXXLanguageFrontend cxxLanguageFrontend =
        new CXXLanguageFrontend(
            file, TranslationConfiguration.builder().build(), new GlobalScope());
    cxxLanguageFrontend.parse(file);

    checkBindings(cxxLanguageFrontend);
  }
}
