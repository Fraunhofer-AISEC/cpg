/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.CallExpression;
import de.fraunhofer.aisec.cpg.graph.ConstructExpression;
import de.fraunhofer.aisec.cpg.graph.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.ExplicitConstructorInvocation;
import de.fraunhofer.aisec.cpg.graph.Expression;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.MemberCallExpression;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.NewExpression;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.ParamVariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.StaticCallExpression;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves {@link CallExpression} and {@link NewExpression} targets.
 *
 * <p>A {@link CallExpression} specifies the method that wants to be called via {@link
 * CallExpression#getName()}. The call target is a method of the same class the caller belongs to,
 * so the name is resolved to the appropriate {@link
 * de.fraunhofer.aisec.cpg.graph.MethodDeclaration}. This pass also takes into consideration that a
 * method might not be present in the current class, but rather has its implementation in a
 * superclass, and sets the pointer accordingly.
 *
 * <p>Constructor calls with {@link NewExpression} are resolved in such a way that their {@link
 * NewExpression#getInstantiates()} points to the correct {@link RecordDeclaration}. Additionally,
 * the {@link ConstructExpression#getConstructor()} is set to the according {@link
 * ConstructorDeclaration}
 */
public class CallResolver implements Pass {

  private static final Logger LOGGER = LoggerFactory.getLogger(CallResolver.class);

  private Map<String, RecordDeclaration> recordMap = new HashMap<>();
  private Map<FunctionDeclaration, Type> containingType = new HashMap<>();
  @Nullable private RecordDeclaration currentClass;
  @Nullable private TranslationUnitDeclaration currentTU;
  private LanguageFrontend lang;

  @Override
  public void cleanup() {
    this.containingType.clear();
    this.currentClass = null;
    this.currentTU = null;
  }

  @Override
  public LanguageFrontend getLang() {
    return lang;
  }

  @Override
  public void setLang(LanguageFrontend lang) {
    this.lang = lang;
  }

  @Override
  public void accept(@NonNull TranslationResult translationResult) {
    ScopedWalker walker = new ScopedWalker();
    walker.registerHandler(this::findRecords);
    walker.registerHandler(this::registerMethods);

    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      walker.iterate(tu);
    }

    walker.clearCallbacks();
    walker.registerHandler(this::resolve);

    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      walker.iterate(tu);
    }
  }

  private void findRecords(@NonNull Node node) {
    if (node instanceof RecordDeclaration) {
      recordMap.putIfAbsent(node.getName(), (RecordDeclaration) node);
    }
  }

  private void registerMethods(@NonNull Type currentClass, Node parent, @NonNull Node currentNode) {
    if (currentNode instanceof MethodDeclaration) {
      containingType.put((FunctionDeclaration) currentNode, currentClass);
    }
  }

  private void resolve(@NonNull Node node) {
    if (node instanceof TranslationUnitDeclaration) {
      this.currentTU = (TranslationUnitDeclaration) node;
    } else if (node instanceof ExplicitConstructorInvocation) {
      ExplicitConstructorInvocation eci = (ExplicitConstructorInvocation) node;
      if (eci.getContainingClass() != null) {
        RecordDeclaration record = recordMap.get(eci.getContainingClass());
        List<Type> signature =
            eci.getArguments().stream().map(Expression::getType).collect(Collectors.toList());
        if (record != null) {
          ConstructorDeclaration constructor = getConstructorDeclaration(signature, record);
          ArrayList<FunctionDeclaration> invokes = new ArrayList<>();
          if (constructor != null) {
            invokes.add(constructor);
          }
          eci.setInvokes(invokes);
        }
      }
    } else if (node instanceof CallExpression) {
      CallExpression call = (CallExpression) node;

      if (this.currentClass == null && this.currentTU != null) {
        // Handle function (not method) calls
        // C++ allows function overloading. Make sure we have at least the same number of arguments
        List<FunctionDeclaration> invocationCandidates =
            currentTU.getDeclarations().stream()
                .filter(FunctionDeclaration.class::isInstance)
                .map(FunctionDeclaration.class::cast)
                .filter(
                    f -> f.getName().equals(call.getName()) && f.hasSignature(call.getSignature()))
                .collect(Collectors.toList());

        call.setInvokes(invocationCandidates);
      } else if (!handlePossibleStaticImport(call)) {
        Set<Type> possibleContainingTypes = getPossibleContainingTypes(node);

        // Find invokes by type
        List<FunctionDeclaration> invocationCandidates =
            call.getInvokes().stream()
                .map(f -> getOverridingCandidates(possibleContainingTypes, f))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Find invokes by supertypes
        if (invocationCandidates.isEmpty()) {
          String[] nameParts = call.getName().split("\\.");
          List<Type> signature = call.getSignature();
          Set<RecordDeclaration> records =
              possibleContainingTypes.stream()
                  .map(t -> recordMap.get(t.getTypeName()))
                  .filter(Objects::nonNull)
                  .collect(Collectors.toSet());
          invocationCandidates =
              getInvocationCandidatesFromParents(
                  nameParts[nameParts.length - 1], signature, records);
        }

        if (!(call instanceof MemberCallExpression || call instanceof StaticCallExpression)) {
          call.setBase(currentClass.getThis());
        }
        call.setInvokes(invocationCandidates);
      }
    } else if (node instanceof NewExpression) {
      // Handle constructor calls
      NewExpression newExpression = (NewExpression) node;
      String typeName = newExpression.getType().getTypeName();
      RecordDeclaration record = recordMap.get(typeName);
      newExpression.setInstantiates(record);
      if (newExpression.getInitializer() instanceof ConstructExpression) {
        ConstructExpression initializer = (ConstructExpression) newExpression.getInitializer();
        List<Type> signature = initializer.getSignature();

        if (record != null) {
          ConstructorDeclaration constructor = getConstructorDeclaration(signature, record);
          if (constructor != null) {
            initializer.setConstructor(constructor);
          } else {
            LOGGER.warn(
                "Unexpected: Could not create constructor for {}.{}", record.getName(), signature);
          }
        }
      }
    } else if (node instanceof RecordDeclaration) {
      currentClass = (RecordDeclaration) node;
    }
  }

  private boolean handlePossibleStaticImport(@Nullable CallExpression call) {
    if (call == null || currentClass == null) {
      return false;
    }
    String name = call.getName().substring(call.getName().lastIndexOf('.') + 1);
    List<FunctionDeclaration> nameMatches =
        currentClass.getStaticImports().stream()
            .filter(FunctionDeclaration.class::isInstance)
            .map(FunctionDeclaration.class::cast)
            .filter(m -> m.getName().equals(name) || m.getName().endsWith("." + name))
            .collect(Collectors.toList());
    if (nameMatches.isEmpty()) {
      return false;
    } else {
      List<FunctionDeclaration> invokes = new ArrayList<>();
      FunctionDeclaration target =
          nameMatches.stream()
              .filter(m -> m.hasSignature(call.getSignature()))
              .findFirst()
              .orElse(null);
      if (target == null) {
        generateDummies(call, name, invokes);
      } else {
        invokes.add(target);
      }

      call.setInvokes(invokes);
      return true;
    }
  }

  private void generateDummies(
      @NonNull CallExpression call,
      @NonNull String name,
      @NonNull List<FunctionDeclaration> invokes) {
    // We had an import for this method name, just not the correct signature. Let's just add
    // a dummy to any class that might be affected
    if (currentClass == null) {
      LOGGER.warn("Cannot generate dummies for imports of a null class: {}", call.toString());
      return;
    }
    List<RecordDeclaration> containingRecords =
        currentClass.getStaticImportStatements().stream()
            .filter(i -> i.endsWith("." + name))
            .map(i -> i.substring(0, i.lastIndexOf('.')))
            .map(c -> recordMap.getOrDefault(c, null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    for (RecordDeclaration record : containingRecords) {
      MethodDeclaration dummy = NodeBuilder.newMethodDeclaration(name, "", true);
      dummy.setDummy(true);
      // prepare signature
      List<ParamVariableDeclaration> params = new ArrayList<>();
      for (int i = 0; i < call.getSignature().size(); i++) {
        Type targetType = call.getSignature().get(i);
        String paramName = generateParamName(i, targetType);
        ParamVariableDeclaration param =
            NodeBuilder.newMethodParameterIn(paramName, targetType, false, "");
        param.setDummy(true);
        param.setArgumentIndex(i);
        params.add(param);
      }
      dummy.setParameters(params);
      record.getMethods().add(dummy);
      currentClass.getStaticImports().add(dummy);
      invokes.add(dummy);
    }
  }

  private String generateParamName(int i, @NonNull Type targetType) {
    StringBuilder paramName = new StringBuilder();
    boolean capitalize = false;
    for (int j = 0; j < targetType.toString().length(); j++) {
      char c = targetType.toString().charAt(j);
      if (c == '.' || c == ':') {
        capitalize = true;
      } else if (c == '*') {
        paramName.append("Ptr");
      } else {
        if (capitalize) {
          paramName.append(String.valueOf(c).toUpperCase());
          capitalize = false;
        } else {
          paramName.append(c);
        }
      }
    }
    paramName.append(i);
    return paramName.toString();
  }

  private Set<Type> getPossibleContainingTypes(Node node) {
    Set<Type> possibleTypes = new HashSet<>();
    if (node instanceof MemberCallExpression) {
      MemberCallExpression memberCall = (MemberCallExpression) node;
      if (memberCall.getBase() instanceof HasType) {
        HasType base = (HasType) memberCall.getBase();
        possibleTypes.add(base.getType());
        possibleTypes.addAll(base.getPossibleSubTypes());
      }
    } else if (node instanceof StaticCallExpression) {
      StaticCallExpression staticCall = (StaticCallExpression) node;
      if (staticCall.getTargetRecord() != null) {
        possibleTypes.add(new Type(staticCall.getTargetRecord()));
      }
    } else if (currentClass != null) {
      possibleTypes.add(new Type(currentClass.getName()));
      possibleTypes.addAll(currentClass.getSuperTypes());
    }
    return possibleTypes;
  }

  private List<FunctionDeclaration> getInvocationCandidatesFromRecord(
      RecordDeclaration record, String name, List<Type> signature) {
    Pattern namePattern =
        Pattern.compile("(" + Pattern.quote(record.getName()) + "\\.)?" + Pattern.quote(name));
    return record.getMethods().stream()
        .filter(m -> namePattern.matcher(m.getName()).matches() && m.hasSignature(signature))
        .map(FunctionDeclaration.class::cast)
        .collect(Collectors.toList());
  }

  private List<FunctionDeclaration> getInvocationCandidatesFromParents(
      String name, List<Type> signature, Set<RecordDeclaration> possibleTypes) {
    if (possibleTypes.isEmpty()) {
      return new ArrayList<>();
    } else {
      List<FunctionDeclaration> firstLevelCandidates =
          possibleTypes.stream()
              .map(r -> getInvocationCandidatesFromRecord(r, name, signature))
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      if (firstLevelCandidates.isEmpty()) {
        return possibleTypes.stream()
            .map(RecordDeclaration::getSuperTypeDeclarations)
            .map(superTypes -> getInvocationCandidatesFromParents(name, signature, superTypes))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
      } else {
        return firstLevelCandidates;
      }
    }
  }

  private Set<FunctionDeclaration> getOverridingCandidates(
      Set<Type> possibleSubTypes, FunctionDeclaration declaration) {
    return declaration.getOverriddenBy().stream()
        .filter(f -> possibleSubTypes.contains(containingType.get(f)))
        .collect(Collectors.toSet());
  }

  @Nullable
  private ConstructorDeclaration getConstructorDeclaration(
      List<Type> signature, RecordDeclaration record) {
    return record.getConstructors().stream()
        .filter(f -> f.hasSignature(signature))
        .findFirst()
        .orElse(null);
  }
}
