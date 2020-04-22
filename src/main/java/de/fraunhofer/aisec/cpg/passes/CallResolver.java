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
import de.fraunhofer.aisec.cpg.graph.CallExpression;
import de.fraunhofer.aisec.cpg.graph.ConstructExpression;
import de.fraunhofer.aisec.cpg.graph.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
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
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.type.FunctionPointerType;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
 * <p>Constructor calls with {@link ConstructExpression} are resolved in such a way that their
 * {@link ConstructExpression#getInstantiates()} points to the correct {@link RecordDeclaration}.
 * Additionally, the {@link ConstructExpression#getConstructor()} is set to the according {@link
 * ConstructorDeclaration}
 */
public class CallResolver extends Pass {

  private static final Logger LOGGER = LoggerFactory.getLogger(CallResolver.class);

  private Map<String, RecordDeclaration> recordMap = new HashMap<>();
  private Map<FunctionDeclaration, Type> containingType = new HashMap<>();
  @Nullable private TranslationUnitDeclaration currentTU;

  @Override
  public void cleanup() {
    this.containingType.clear();
    this.currentTU = null;
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
    walker.registerHandler(this::fixInitializers);

    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      walker.iterate(tu);
    }

    walker.clearCallbacks();
    walker.registerHandler(this::resolve);

    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      walker.iterate(tu);
    }
  }

  private void findRecords(@NonNull Node node, RecordDeclaration curClass) {
    if (node instanceof RecordDeclaration) {
      recordMap.putIfAbsent(node.getName(), (RecordDeclaration) node);
    }
  }

  private void registerMethods(
      RecordDeclaration currentClass, Node parent, @NonNull Node currentNode) {
    if (currentNode instanceof MethodDeclaration && currentClass != null) {
      containingType.put(
          (FunctionDeclaration) currentNode, TypeParser.createFrom(currentClass.getName()));
    }
  }

  private void fixInitializers(@NonNull Node node, RecordDeclaration curClass) {
    if (node instanceof VariableDeclaration) {
      VariableDeclaration declaration = ((VariableDeclaration) node);
      // check if we have the corresponding class for this type
      String typeString = declaration.getType().toString();
      // pointer is also okay
      if (StringUtils.countMatches(typeString, '*') == 1) {
        typeString = typeString.replace("*", "");
      }
      boolean isRecord = recordMap.containsKey(typeString);

      if (isRecord) {
        Expression currInitializer = declaration.getInitializer();
        if (currInitializer == null && declaration.isImplicitInitializerAllowed()) {
          ConstructExpression initializer = NodeBuilder.newConstructExpression("()");
          initializer.setImplicit(true);
          declaration.setInitializer(initializer);
        } else if (currInitializer instanceof CallExpression
            && currInitializer.getName().equals(typeString)) {
          // This should actually be a construct expression, not a call!
          CallExpression call = (CallExpression) currInitializer;
          List<Expression> arguments = call.getArguments();
          String signature =
              arguments.stream().map(Node::getCode).collect(Collectors.joining(", "));
          ConstructExpression initializer =
              NodeBuilder.newConstructExpression("(" + signature + ")");
          initializer.setArguments(new ArrayList<>(arguments));
          initializer.setImplicit(true);
          declaration.setInitializer(initializer);
          currInitializer.disconnectFromGraph();
        }
      }
    } else if (node instanceof NewExpression) {
      NewExpression newExpression = (NewExpression) node;
      if (newExpression.getInitializer() == null) {
        ConstructExpression initializer = NodeBuilder.newConstructExpression("()");
        initializer.setImplicit(true);
        newExpression.setInitializer(initializer);
      }
    }
  }

  private void resolve(@NonNull Node node, RecordDeclaration curClass) {
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

      if (call instanceof MemberCallExpression) {
        Node member = ((MemberCallExpression) call).getMember();
        if (member instanceof HasType
            && ((HasType) member).getType() instanceof FunctionPointerType) {
          List<FunctionDeclaration> invocationCandidates = new ArrayList<>();
          Deque<Node> worklist = new ArrayDeque<>();
          Set<Node> seen = Collections.newSetFromMap(new IdentityHashMap<>());
          worklist.push(member);
          DeclaredReferenceExpression finalReference = null;
          while (!worklist.isEmpty()) {
            Node curr = worklist.pop();
            if (!seen.add(curr)) {
              continue;
            }
            if (curr instanceof FunctionDeclaration) {
              if (((FunctionDeclaration) curr).hasSignature(call.getSignature())) {
                invocationCandidates.add((FunctionDeclaration) curr);
              } else if (curr.isImplicit()) {
                // unknown function, so even if its signature does not fit, we need to make a new
                // dummy that does match
                if (((FunctionDeclaration) curr).hasSignature(call.getSignature())) {
                  invocationCandidates.add((FunctionDeclaration) curr);
                  // refine the referral pointer
                  if (finalReference != null && finalReference.getRefersTo().contains(curr)) {
                    finalReference.setRefersTo((ValueDeclaration) curr);
                  }
                } else {
                  FunctionDeclaration dummy =
                      createDummyWithMatchingSignature(
                          (FunctionDeclaration) curr, call.getSignature());
                  invocationCandidates.add(dummy);
                  // redirect the referral pointer
                  if (finalReference != null && finalReference.getRefersTo().contains(curr)) {
                    finalReference.setRefersTo(dummy);
                  }
                }
              }
            } else {
              if (curr instanceof DeclaredReferenceExpression) {
                finalReference = (DeclaredReferenceExpression) curr;
              }
              curr.getPrevDFG().forEach(worklist::push);
            }
          }
          call.setInvokes(invocationCandidates);
          return;
        }
      }

      if (curClass == null && this.currentTU != null) {
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
      } else if (!handlePossibleStaticImport(call, curClass)) {
        Set<Type> possibleContainingTypes = getPossibleContainingTypes(node, curClass);

        // Find invokes by type
        List<FunctionDeclaration> invocationCandidates =
            call.getInvokes().stream()
                .map(f -> getOverridingCandidates(possibleContainingTypes, f))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Find invokes by supertypes
        if (invocationCandidates.isEmpty()) {
          String[] nameParts = call.getName().split("\\.");
          if (nameParts.length > 0) {
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
        }

        if (curClass != null
            && !(call instanceof MemberCallExpression || call instanceof StaticCallExpression)) {
          call.setBase(curClass.getThis());
        }
        call.setInvokes(invocationCandidates);
      }
    } else if (node instanceof ConstructExpression) {
      ConstructExpression constructExpression = (ConstructExpression) node;
      List<Type> signature = constructExpression.getSignature();
      String typeName = constructExpression.getType().getTypeName();
      RecordDeclaration record = recordMap.get(typeName);
      constructExpression.setInstantiates(record);

      if (record != null && record.getCode() != null && !record.getCode().isEmpty()) {
        ConstructorDeclaration constructor = getConstructorDeclaration(signature, record);
        if (constructor != null) {
          constructExpression.setConstructor(constructor);
        } else {
          LOGGER.warn(
              "Unexpected: Could not find constructor for {} with signature {}",
              record.getName(),
              signature);
        }
      }
    }
  }

  private boolean handlePossibleStaticImport(
      @Nullable CallExpression call, RecordDeclaration curClass) {
    if (call == null || curClass == null) {
      return false;
    }
    String name = call.getName().substring(call.getName().lastIndexOf('.') + 1);
    List<FunctionDeclaration> nameMatches =
        curClass.getStaticImports().stream()
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
        generateStaticImportDummies(call, name, invokes, curClass);
      } else {
        invokes.add(target);
      }

      call.setInvokes(invokes);
      return true;
    }
  }

  private void generateStaticImportDummies(
      @NonNull CallExpression call,
      @NonNull String name,
      @NonNull List<FunctionDeclaration> invokes,
      RecordDeclaration curClass) {
    // We had an import for this method name, just not the correct signature. Let's just add
    // a dummy to any class that might be affected
    if (curClass == null) {
      LOGGER.warn("Cannot generate dummies for imports of a null class: {}", call.toString());
      return;
    }
    List<RecordDeclaration> containingRecords =
        curClass.getStaticImportStatements().stream()
            .filter(i -> i.endsWith("." + name))
            .map(i -> i.substring(0, i.lastIndexOf('.')))
            .map(c -> recordMap.getOrDefault(c, null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    for (RecordDeclaration record : containingRecords) {
      MethodDeclaration dummy = NodeBuilder.newMethodDeclaration(name, "", true, record);
      dummy.setImplicit(true);
      List<ParamVariableDeclaration> params = createParameters(call.getSignature());
      dummy.setParameters(params);
      record.getMethods().add(dummy);
      curClass.getStaticImports().add(dummy);
      invokes.add(dummy);
    }
  }

  private Optional<FunctionDeclaration> checkExistingDummies(
      FunctionDeclaration template, List<Type> signature) {
    if (template instanceof MethodDeclaration
        && ((MethodDeclaration) template).getRecordDeclaration() != null) {
      return ((MethodDeclaration) template)
          .getRecordDeclaration().getMethods().stream()
              .filter(m -> m.getName().equals(template.getName()) && m.hasSignature(signature))
              .map(FunctionDeclaration.class::cast)
              .findFirst();
    } else {
      if (currentTU == null) {
        LOGGER.error(
            "No current translation unit when trying to find matching dummy for {}", template);
        return Optional.empty();
      }
      return currentTU.getDeclarations().stream()
          .filter(FunctionDeclaration.class::isInstance)
          .map(FunctionDeclaration.class::cast)
          .filter(f -> f.getName().equals(template.getName()) && f.hasSignature(signature))
          .findFirst();
    }
  }

  private FunctionDeclaration createDummyWithMatchingSignature(
      FunctionDeclaration template, List<Type> signature) {
    Optional<FunctionDeclaration> existing = checkExistingDummies(template, signature);
    if (existing.isPresent()) {
      return existing.get();
    }

    List<ParamVariableDeclaration> parameters = createParameters(signature);
    if (template instanceof MethodDeclaration) {
      RecordDeclaration containingRecord = ((MethodDeclaration) template).getRecordDeclaration();
      MethodDeclaration dummy =
          NodeBuilder.newMethodDeclaration(
              template.getName(),
              template.getCode(),
              ((MethodDeclaration) template).isStatic(),
              containingRecord);
      dummy.setImplicit(true);
      dummy.setParameters(parameters);

      if (containingRecord == null) {
        // not inside a class, lets put it inside the translation unit
        if (currentTU == null) {
          LOGGER.error(
              "No current translation unit when trying to generate method dummy {}",
              dummy.getName());
        } else {
          currentTU.getDeclarations().add(dummy);
        }
      } else {
        containingRecord.getMethods().add(dummy);
      }
      return dummy;
    } else {
      // function declaration, not inside a class
      FunctionDeclaration dummy =
          NodeBuilder.newFunctionDeclaration(template.getName(), template.getCode());
      dummy.setParameters(parameters);
      dummy.setImplicit(true);
      if (currentTU == null) {
        LOGGER.error(
            "No current translation unit when trying to generate function dummy {}",
            dummy.getName());
      } else {
        currentTU.getDeclarations().add(dummy);
      }
      return dummy;
    }
  }

  private List<ParamVariableDeclaration> createParameters(List<Type> signature) {
    List<ParamVariableDeclaration> params = new ArrayList<>();
    for (int i = 0; i < signature.size(); i++) {
      Type targetType = signature.get(i);
      String paramName = generateParamName(i, targetType);
      ParamVariableDeclaration param =
          NodeBuilder.newMethodParameterIn(paramName, targetType, false, "");
      param.setImplicit(true);
      param.setArgumentIndex(i);
      params.add(param);
    }
    return params;
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

  private Set<Type> getPossibleContainingTypes(Node node, RecordDeclaration curClass) {
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
        possibleTypes.add(TypeParser.createFrom(staticCall.getTargetRecord()));
      }
    } else if (curClass != null) {
      possibleTypes.add(TypeParser.createFrom(curClass.getName()));
      possibleTypes.addAll(curClass.getSuperTypes());
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
