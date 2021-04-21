/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.passes;

import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaExternalTypeHierarchyResolver extends Pass {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(JavaExternalTypeHierarchyResolver.class);

  @Override
  public void accept(TranslationResult translationResult) {
    // Run only for Java.
    if (this.lang instanceof JavaLanguageFrontend) {
      TypeSolver resolver = ((JavaLanguageFrontend) this.lang).getNativeTypeResolver();
      TypeManager tm = TypeManager.getInstance();

      // Iterate over all known types and add their (direct) supertypes.
      for (Type t : new HashSet<>(tm.getFirstOrderTypes())) {
        SymbolReference<ResolvedReferenceTypeDeclaration> symbol =
            resolver.tryToSolveType(t.getTypeName());
        if (symbol.isSolved()) {
          try {
            List<ResolvedReferenceType> resolvedSuperTypes =
                symbol.getCorrespondingDeclaration().getAncestors(true);
            for (ResolvedReferenceType anc : resolvedSuperTypes) {
              // Add all resolved supertypes to the type.
              Type superType = TypeParser.createFrom(anc.getQualifiedName(), false);
              superType.setTypeOrigin(Type.Origin.RESOLVED);
              t.getSuperTypes().add(superType);
            }
          } catch (UnsolvedSymbolException e) {
            // Even if the symbol itself is resolved, "getAnchestors()" may throw exception.
            LOGGER.warn("Could not resolve supertypes of {}", symbol.getCorrespondingDeclaration());
          }
        }
      }
    }
  }

  @Override
  public void cleanup() {
    // nothing to do here.
  }
}
