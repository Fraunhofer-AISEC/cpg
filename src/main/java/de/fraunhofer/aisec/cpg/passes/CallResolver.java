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
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declaration.*;
import de.fraunhofer.aisec.cpg.graph.statement.expression.*;
import de.fraunhofer.aisec.cpg.graph.type.FunctionPointerType;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy;
import java.util.*;
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
 * so the name is resolved to the appropriate {@link MethodDeclaration}. This pass also takes into
 * consideration that a method might not be present in the current class, but rather has its
 * implementation in a superclass, and sets the pointer accordingly.
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
  private ScopedWalker walker;

  @Override
  public void cleanup() {
    this.containingType.clear();
    this.currentTU = null;
  }

  @Override
  public void accept(@NonNull TranslationResult translationResult) {
    walker = new ScopedWalker();
    walker.registerHandler((currClass, parent, currNode) -> walker.collectDeclarations(currNode));
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
          (FunctionDeclaration) currentNode, TypeParser.createFrom(currentClass.getName(), true));
    }
  }

  private void fixInitializers(@NonNull Node node, RecordDeclaration curClass) {
    if (node instanceof VariableDeclaration) {
      VariableDeclaration declaration = ((VariableDeclaration) node);
      // check if we have the corresponding class for this type
      String typeString = declaration.getType().getRoot().getName();
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

  /**
   * Handle calls in the form of <code>super.call()</code> or <code>ClassName.super.call()
   * </code>, conforming to JLS13 §15.12.1
   *
   * @param curClass The class containing the call
   * @param call The call to be resolved
   */
  private void handleSuperCall(RecordDeclaration curClass, CallExpression call) {
    RecordDeclaration target = null;
    if (call.getBase().getName().equals("super")) {
      // direct superclass, either defined explicitly or java.lang.Object by default
      if (!curClass.getSuperClasses().isEmpty()) {
        target = recordMap.get(curClass.getSuperClasses().get(0).getTypeName());
      } else {
        Util.warnWithFileLocation(
            call,
            LOGGER,
            "super call without direct superclass! Expected "
                + "java.lang.Object to be present at least!");
      }
    } else {
      // BaseName.super.call(), might either be in order to specify an enclosing class or an
      // interface that is implemented
      target = handleSpecificSupertype(curClass, call);
    }
    if (target != null) {
      ((DeclaredReferenceExpression) call.getBase()).setRefersTo(target.getThis());
      handleMethodCall(target, call);
    }
  }

  private RecordDeclaration handleSpecificSupertype(
      RecordDeclaration curClass, CallExpression call) {
    String baseName =
        call.getBase().getName().substring(0, call.getBase().getName().lastIndexOf(".super"));
    if (curClass.getImplementedInterfaces().contains(TypeParser.createFrom(baseName, true))) {
      // Basename is an interface -> BaseName.super refers to BaseName itself
      return recordMap.get(baseName);
    } else {
      // BaseName refers to an enclosing class -> BaseName.super is BaseName's superclass
      RecordDeclaration base = recordMap.get(baseName);
      if (base != null) {
        if (!base.getSuperClasses().isEmpty()) {
          return recordMap.get(base.getSuperClasses().get(0).getTypeName());
        } else {
          Util.warnWithFileLocation(
              call,
              LOGGER,
              "super call without direct superclass! Expected "
                  + "java.lang.Object to be present at least!");
        }
      }
    }
    return null;
  }

  private void resolve(@NonNull Node node, RecordDeclaration curClass) {
    if (node instanceof TranslationUnitDeclaration) {
      this.currentTU = (TranslationUnitDeclaration) node;
    } else if (node instanceof ExplicitConstructorInvocation) {
      resolveExplicitConstructorInvocation((ExplicitConstructorInvocation) node);
    } else if (node instanceof CallExpression) {
      CallExpression call = (CallExpression) node;
      // We might have call expressions inside our arguments, so in order to correctly resolve
      // this call's signature, we need to make sure any call expression arguments are fully
      // resolved
      resolveArguments(call, curClass);
      handleCallExpression(curClass, call);
    } else if (node instanceof ConstructExpression) {
      resolveConstructExpression((ConstructExpression) node);
    }
  }

  private void handleCallExpression(RecordDeclaration curClass, CallExpression call) {
    if (lang instanceof JavaLanguageFrontend
        && call.getBase() instanceof DeclaredReferenceExpression
        && call.getBase().getName().matches("(?<class>.+\\.)?super")) {
      handleSuperCall(curClass, call);
      return;
    }

    if (call instanceof MemberCallExpression) {
      Node member = ((MemberCallExpression) call).getMember();
      if (member instanceof HasType
          && ((HasType) member).getType() instanceof FunctionPointerType) {
        handleFunctionPointerCall(call, member);
        return;
      }
    }

    // we could be referring to a function pointer even though it is not a member call if the
    // usual function pointer syntax (*fp)() has been omitted: fp(). Looks like a normal call,
    // but it isn't
    Optional<? extends ValueDeclaration> funcPointer =
        walker.getDeclarationForScope(
            call,
            v -> v.getType() instanceof FunctionPointerType && v.getName().equals(call.getName()));
    if (funcPointer.isPresent()) {
      handleFunctionPointerCall(call, funcPointer.get());
    } else {
      handleNormalCalls(curClass, call);
    }
  }

  private void resolveArguments(CallExpression call, RecordDeclaration curClass) {
    Deque<Node> worklist = new ArrayDeque<>();
    call.getArguments().forEach(worklist::push);
    while (!worklist.isEmpty()) {
      Node curr = worklist.pop();
      if (curr instanceof CallExpression) {
        resolve(curr, curClass);
      } else {
        Iterator<Node> it = Strategy.AST_FORWARD(curr);
        while (it.hasNext()) {
          Node astChild = it.next();
          if (!(astChild instanceof RecordDeclaration)) {
            worklist.push(astChild);
          }
        }
      }
    }
  }

  private void handleNormalCalls(RecordDeclaration curClass, CallExpression call) {
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
      if (invocationCandidates.isEmpty()) {
        invocationCandidates =
            List.of(createDummy(null, call.getName(), call.getCode(), false, call.getSignature()));
      }

      call.setInvokes(invocationCandidates);
    } else if (!handlePossibleStaticImport(call, curClass)) {
      handleMethodCall(curClass, call);
    }
  }

  private void handleMethodCall(RecordDeclaration curClass, CallExpression call) {
    Set<Type> possibleContainingTypes = getPossibleContainingTypes(call, curClass);

    // Find overridden invokes
    List<FunctionDeclaration> invocationCandidates =
        call.getInvokes().stream()
            .map(f -> getOverridingCandidates(possibleContainingTypes, f))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    // Find function targets
    if (invocationCandidates.isEmpty() && currentTU != null) {
      invocationCandidates =
          currentTU.getDeclarations().stream()
              .filter(FunctionDeclaration.class::isInstance)
              .map(FunctionDeclaration.class::cast)
              .filter(
                  f -> f.getName().equals(call.getName()) && f.hasSignature(call.getSignature()))
              .collect(Collectors.toList());
    }

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
            getInvocationCandidatesFromParents(nameParts[nameParts.length - 1], signature, records);
      }
    }

    if (curClass != null
        && !(call instanceof MemberCallExpression || call instanceof StaticCallExpression)) {
      call.setBase(curClass.getThis());
    }

    if (invocationCandidates.isEmpty()) {
      possibleContainingTypes.stream()
          .map(t -> recordMap.get(t.getTypeName()))
          .filter(Objects::nonNull)
          .map(r -> createDummy(r, call.getName(), call.getCode(), false, call.getSignature()))
          .forEach(invocationCandidates::add);
    }
    call.setInvokes(invocationCandidates);
  }

  private void resolveConstructExpression(ConstructExpression constructExpression) {
    List<Type> signature = constructExpression.getSignature();
    String typeName = constructExpression.getType().getTypeName();
    RecordDeclaration record = recordMap.get(typeName);
    constructExpression.setInstantiates(record);

    if (record != null && record.getCode() != null && !record.getCode().isEmpty()) {
      ConstructorDeclaration constructor = getConstructorDeclaration(signature, record);
      constructExpression.setConstructor(constructor);
    }
  }

  private void handleFunctionPointerCall(CallExpression call, Node pointer) {
    if (!(pointer instanceof HasType
        && ((HasType) pointer).getType() instanceof FunctionPointerType)) {
      LOGGER.error("Can't handle a function pointer call without function pointer type");
      return;
    }
    FunctionPointerType pointerType = (FunctionPointerType) ((HasType) pointer).getType();
    List<FunctionDeclaration> invocationCandidates = new ArrayList<>();
    Deque<Node> worklist = new ArrayDeque<>();
    Set<Node> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    worklist.push(pointer);
    while (!worklist.isEmpty()) {
      Node curr = worklist.pop();
      if (!seen.add(curr)) {
        continue;
      }
      if (curr instanceof FunctionDeclaration) {
        FunctionDeclaration f = (FunctionDeclaration) curr;
        // Even if it is a function declaration, the dataflow might just come from a situation
        // where the target of a fptr is passed through via a return value. Keep searching if
        // return type or signature don't match
        if (TypeManager.getInstance().isSupertypeOf(pointerType.getReturnType(), f.getType())
            && f.hasSignature(pointerType.getParameters())) {
          invocationCandidates.add((FunctionDeclaration) curr);
          // We have found a target. Don't follow this path any further, but still continue the
          // other paths that might be left, as we could have several potential targets at runtime
          continue;
        }
      }
      curr.getPrevDFG().forEach(worklist::push);
    }
    call.setInvokes(invocationCandidates);
  }

  private void resolveExplicitConstructorInvocation(ExplicitConstructorInvocation eci) {
    if (eci.getContainingClass() != null) {
      RecordDeclaration record = recordMap.get(eci.getContainingClass());
      List<Type> signature =
          eci.getArguments().stream().map(Expression::getType).collect(Collectors.toList());
      if (record != null) {
        ConstructorDeclaration constructor = getConstructorDeclaration(signature, record);
        ArrayList<FunctionDeclaration> invokes = new ArrayList<>();
        invokes.add(constructor);
        eci.setInvokes(invokes);
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
      List<ParamVariableDeclaration> params = Util.createParameters(call.getSignature());
      dummy.setParameters(params);
      record.getMethods().add(dummy);
      curClass.getStaticImports().add(dummy);
      invokes.add(dummy);
    }
  }

  @NonNull
  private FunctionDeclaration createDummy(
      RecordDeclaration containingRecord,
      String name,
      String code,
      boolean isStatic,
      List<Type> signature) {

    List<ParamVariableDeclaration> parameters = Util.createParameters(signature);
    if (containingRecord != null) {
      MethodDeclaration dummy =
          NodeBuilder.newMethodDeclaration(name, code, isStatic, containingRecord);
      dummy.setImplicit(true);
      dummy.setParameters(parameters);

      containingRecord.getMethods().add(dummy);
      return dummy;
    } else {
      // function declaration, not inside a class
      FunctionDeclaration dummy = NodeBuilder.newFunctionDeclaration(name, code);
      dummy.setParameters(parameters);
      dummy.setImplicit(true);
      if (currentTU == null) {
        LOGGER.error(
            "No current translation unit when trying to generate function dummy {}",
            dummy.getName());
      } else {
        currentTU.add(dummy);
      }
      return dummy;
    }
  }

  private ConstructorDeclaration createConstructorDummy(
      @NonNull RecordDeclaration containingRecord, List<Type> signature) {
    ConstructorDeclaration dummy =
        NodeBuilder.newConstructorDeclaration(containingRecord.getName(), "", containingRecord);
    dummy.setImplicit(true);
    dummy.setParameters(Util.createParameters(signature));
    containingRecord.getConstructors().add(dummy);
    return dummy;
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
        possibleTypes.add(TypeParser.createFrom(staticCall.getTargetRecord(), true));
      }
    } else if (curClass != null) {
      possibleTypes.add(TypeParser.createFrom(curClass.getName(), true));
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

  @NonNull
  private ConstructorDeclaration getConstructorDeclaration(
      List<Type> signature, RecordDeclaration record) {
    return record.getConstructors().stream()
        .filter(f -> f.hasSignature(signature))
        .findFirst()
        .orElseGet(() -> createConstructorDummy(record, signature));
  }
}
