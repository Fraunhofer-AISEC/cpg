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
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
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
    walker = new ScopedWalker(lang);
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
      } else {
        handleMethodCall(curClass, call);
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

  /**
   * @param callSignature Type signature of the CallExpression
   * @param functionSignature Type signature of the FunctionDeclaration
   * @return true if the CallExpression signature can be transformed into the FunctionDeclaration
   *     signature by means of casting
   */
  private boolean compatibleSignatures(List<Type> callSignature, List<Type> functionSignature) {
    if (callSignature.size() == functionSignature.size()) {
      for (int i = 0; i < callSignature.size(); i++) {
        if ((callSignature.get(i).isPrimitive() != functionSignature.get(i).isPrimitive())
            && !TypeManager.getInstance()
                .isSupertypeOf(functionSignature.get(i), callSignature.get(i))) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Computes the implicit casts that are necessary to reach the
   *
   * @param callSignature signature of the call we want to find invocation targets for by performing
   *     implicit casts
   * @param arguments arguments of the call
   * @param functionSignature Types of the signature of the possible invocation candidate
   * @return List containing either null on the i-th position (if the type of i-th argument of the
   *     call equals the type of the i-th argument of the FunctionDeclaration) or a CastExpression
   *     on the i-th position (if the argument of the call can be casted to match the type of the
   *     argument at the i-th position of the FunctionDeclaration). If the list is empty the
   *     signature of the FunctionDeclaration cannot be reached through implicit casts
   */
  private List<CastExpression> signatureWithImplicitCastTransformation(
      List<Type> callSignature, List<Expression> arguments, List<Type> functionSignature) {
    if (callSignature.size() == functionSignature.size()) {
      List<CastExpression> implicitCasts = new ArrayList<>();

      for (int i = 0; i < callSignature.size(); i++) {
        Type callType = callSignature.get(i);
        Type funcType = functionSignature.get(i);
        if (callType.isPrimitive() && funcType.isPrimitive() && !(callType.equals(funcType))) {
          CastExpression implicitCast = new CastExpression();
          implicitCast.setImplicit(true);
          implicitCast.setCastType(funcType);
          implicitCast.setExpression(arguments.get(i));
          implicitCasts.add(implicitCast);
        } else {
          // If no cast is needed we add null to be able to access the function signature list and
          // the implicit cast list with the same index.
          implicitCasts.add(null);
        }
      }

      return implicitCasts;
    }
    return new ArrayList<>();
  }

  /**
   * @param call CallExpression
   * @param functionDeclaration FunctionDeclaration the CallExpression was resolved to
   * @return list containing the signature containing all argument types including the default
   *     arguments
   */
  private List<Type> getCallSignatureWithDefaults(
      CallExpression call, FunctionDeclaration functionDeclaration) {
    List<Type> callSignature = new ArrayList<>(call.getSignature());
    if (call.getSignature().size() < functionDeclaration.getParameters().size()) {
      callSignature.addAll(
          functionDeclaration
              .getDefaultParameterSignature()
              .subList(
                  call.getArguments().size(),
                  functionDeclaration.getDefaultParameterSignature().size()));
    }

    return callSignature;
  }

  /**
   * @param constructExpression ConstructExpression
   * @param constructorDeclaration ConstructorDeclaration the ConstructExpression was resolved to
   * @return list containing the signature containing all argument types including the default
   *     arguments
   */
  private List<Type> getCallSignatureWithDefaults(
      ConstructExpression constructExpression, ConstructorDeclaration constructorDeclaration) {
    List<Type> callSignature = new ArrayList<>(constructExpression.getSignature());
    if (constructExpression.getSignature().size() < constructorDeclaration.getParameters().size()) {
      callSignature.addAll(
          constructorDeclaration
              .getDefaultParameterSignature()
              .subList(
                  constructExpression.getArguments().size(),
                  constructorDeclaration.getDefaultParameterSignature().size()));
    }

    return callSignature;
  }

  /**
   * Adds the implicit default arguments to the CallExpression if they were not provided
   *
   * @param functionDeclaration the CallExpression has been resolved to containing the default
   *     arguments
   * @param call CallExpression which does not contain all necessary arguments and uses the default
   *     arguments
   */
  private void addDefaultArgsToCall(FunctionDeclaration functionDeclaration, CallExpression call) {
    if (functionDeclaration.hasSignature(getCallSignatureWithDefaults(call, functionDeclaration))) {
      for (Expression expression :
          functionDeclaration
              .getDefaultParameters()
              .subList(
                  call.getArguments().size(), functionDeclaration.getDefaultParameters().size())) {
        call.addArgument(expression, true);
      }
    }
  }

  /**
   * Adds the implicit default arguments to the CallExpression if they were not provided
   *
   * @param constructorDeclaration the ConstructExpression has been resolved to containing the
   *     default arguments
   * @param constructExpression ConstructExpression which does not contain all necessary arguments
   *     and uses the default arguments
   */
  private void addDefaultArgsToCall(
      ConstructorDeclaration constructorDeclaration, ConstructExpression constructExpression) {
    if (constructorDeclaration.hasSignature(
        getCallSignatureWithDefaults(constructExpression, constructorDeclaration))) {
      for (Expression expression :
          constructorDeclaration
              .getDefaultParameters()
              .subList(
                  constructExpression.getArguments().size(),
                  constructorDeclaration.getDefaultParameters().size())) {
        constructExpression.addArgument(expression, true);
      }
    }
  }

  /**
   * modifies: call arguments by applying implicit casts
   *
   * @param call we want to find invocation targets for by performing implicit casts
   * @return list of invocation candidates by applying
   */
  private List<FunctionDeclaration> resolveWithImplicitCast(CallExpression call) {
    // Get possible invocation targets based on the function name
    assert currentTU != null;
    List<FunctionDeclaration> matchingFunctionName =
        currentTU.getDeclarations().stream()
            .filter(FunctionDeclaration.class::isInstance)
            .map(FunctionDeclaration.class::cast)
            .filter(f -> f.getName().equals(call.getName()) && !f.isImplicit())
            .collect(Collectors.toList());

    // Output list for invocationTargets obtaining a valid signature by performing implicit casts
    List<FunctionDeclaration> invocationTargetsWithImplicitCast = new ArrayList<>();
    List<FunctionDeclaration> invocationTargetsWithImplicitCastAndDefaults = new ArrayList<>();

    List<CastExpression> implicitCasts = null;
    List<Type> callSignature;

    // Iterate through all possible invocation candidates
    for (FunctionDeclaration functionDeclaration : matchingFunctionName) {
      callSignature = getCallSignatureWithDefaults(call, functionDeclaration);
      // Check if the signatures match by implicit casts
      if (compatibleSignatures(callSignature, functionDeclaration.getSignatureTypes())) {
        List<CastExpression> implicitCastTargets =
            signatureWithImplicitCastTransformation(
                getCallSignatureWithDefaults(call, functionDeclaration),
                call.getArguments(),
                functionDeclaration.getSignatureTypes());
        if (implicitCasts == null) {
          implicitCasts = implicitCastTargets;
        } else {
          // Since we can have multiple possible invocation targets the cast must all be to the same
          // target type
          checkMostCommonImplicitCast(implicitCasts, implicitCastTargets);
        }
        if (compatibleSignatures(call.getSignature(), functionDeclaration.getSignatureTypes())) {
          invocationTargetsWithImplicitCast.add(functionDeclaration);
        } else {
          invocationTargetsWithImplicitCastAndDefaults.add(functionDeclaration);
        }
      }
    }

    // Apply implicit casts to call arguments
    applyImplicitCastToArguments(call, implicitCasts);

    // Prio implicit casts without defaults
    if (!invocationTargetsWithImplicitCast.isEmpty()) {
      return invocationTargetsWithImplicitCast;
    }

    // Apply default arguments
    for (FunctionDeclaration functionDecl : invocationTargetsWithImplicitCastAndDefaults) {
      addDefaultArgsToCall(functionDecl, call);
    }

    return invocationTargetsWithImplicitCastAndDefaults;
  }

  /**
   * Checks if the current casts are compatible with the casts necessary to match with a new
   * FunctionDeclaration. If a one argument would need to be casted in two different types it would
   * be modified to a cast to UnknownType
   *
   * @param implicitCasts current Cast
   * @param implicitCastTargets new Cast
   */
  private void checkMostCommonImplicitCast(
      List<CastExpression> implicitCasts, List<CastExpression> implicitCastTargets) {
    for (int i = 0; i < implicitCasts.size(); i++) {
      CastExpression currentCast = implicitCasts.get(i);
      CastExpression otherCast = implicitCastTargets.get(i);
      if (currentCast != null && otherCast != null && !(currentCast.equals(otherCast))) {
        // If we have multiple function targets with different implicit casts we have an
        // ambiguous call and we can't have a single cast
        CastExpression contradictoryCast = new CastExpression();
        contradictoryCast.setImplicit(true);
        contradictoryCast.setCastType(UnknownType.getUnknownType());
        contradictoryCast.setExpression(currentCast.getExpression());
        implicitCasts.set(i, contradictoryCast);
      }
    }
  }

  /**
   * Changes the arguments of the CallExpression to use the implcit casts instead
   *
   * @param call CallExpression
   * @param implicitCasts Casts
   */
  private void applyImplicitCastToArguments(
      CallExpression call, List<CastExpression> implicitCasts) {
    if (implicitCasts != null) {
      for (int i = 0; i < implicitCasts.size(); i++) {
        if (implicitCasts.get(i) != null) {
          call.setArgument(i, implicitCasts.get(i));
        }
      }
    }
  }

  /**
   * Changes the arguments of the ConstructExpression to use the implcit casts instead
   *
   * @param constructExpression ConstructExpression
   * @param implicitCasts Casts
   */
  private void applyImplicitCastToArguments(
      ConstructExpression constructExpression, List<CastExpression> implicitCasts) {
    if (implicitCasts != null) {
      for (int i = 0; i < implicitCasts.size(); i++) {
        if (implicitCasts.get(i) != null) {
          constructExpression.setArgument(i, implicitCasts.get(i));
        }
      }
    }
  }

  /**
   * Resolves a CallExpression to the potential target FunctionDeclarations by checking for ommitted
   * arguments due to previously defined default arguments
   *
   * @param call CallExpression
   * @return List of FunctionDeclarations that are the target of the CallExpression (will be
   *     connected with an invokes edge)
   */
  private List<FunctionDeclaration> resolveWithDefaultArgs(CallExpression call) {
    assert currentTU != null;
    List<FunctionDeclaration> invocationCandidates =
        currentTU.getDeclarations().stream()
            .filter(FunctionDeclaration.class::isInstance)
            .map(FunctionDeclaration.class::cast)
            .filter(
                f ->
                    f.getName().equals(call.getName())
                        && !f.isImplicit()
                        && call.getSignature().size() < f.getSignatureTypes().size())
            .collect(Collectors.toList());
    List<FunctionDeclaration> invocationCandidatesDefaultArgs = new ArrayList<>();

    for (FunctionDeclaration functionDeclaration : invocationCandidates) {
      addDefaultArgsToCall(functionDeclaration, call);
      if (functionDeclaration.hasSignature(call.getSignature())) {
        invocationCandidatesDefaultArgs.add(functionDeclaration);
      }
    }
    return invocationCandidatesDefaultArgs;
  }

  private void handleNormalCalls(RecordDeclaration curClass, CallExpression call) {
    if (curClass == null && this.currentTU != null) {
      // Handle function (not method) calls
      // C++ allows function overloading. Make sure we have at least the same number of arguments

      var invocationCandidates = lang.getScopeManager().resolveFunction(call);

      if (invocationCandidates.isEmpty() && this.getLang() instanceof CXXLanguageFrontend) {
        // Check for usage of default args
        invocationCandidates.addAll(resolveWithDefaultArgs(call));
      }

      if (invocationCandidates.isEmpty() && this.getLang() instanceof CXXLanguageFrontend) {
        // If we don't find any candidate and our current language is c/c++ we check if there is a
        // candidate with an implicit cast
        invocationCandidates.addAll(resolveWithImplicitCast(call));
      }

      if (invocationCandidates.isEmpty()) {
        // If we still have no candidates and our current language is c++ we create dummy
        // FunctionDeclaration
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
    String typeName = constructExpression.getType().getTypeName();
    RecordDeclaration record = recordMap.get(typeName);
    constructExpression.setInstantiates(record);

    if (record != null && record.getCode() != null && !record.getCode().isEmpty()) {
      ConstructorDeclaration constructor =
          getConstructorDeclarationCXX(constructExpression, record);
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
        ConstructorDeclaration constructor =
            getConstructorDeclarationForExplicitInvocation(signature, record);
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
      record.addMethod(dummy);
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

      containingRecord.addMethod(dummy);
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
        currentTU.addDeclaration(dummy);
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
    containingRecord.addConstructor(dummy);
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

  private ConstructorDeclaration getConstructorDeclarationDirectMatch(
      List<Type> signature, RecordDeclaration recordDeclaration) {
    for (ConstructorDeclaration constructor : recordDeclaration.getConstructors()) {
      if (constructor.hasSignature(signature)) {
        return constructor;
      }
    }
    return null;
  }

  private ConstructorDeclaration resolveConstructorWithDefaults(
      ConstructExpression constructExpression, List<Type> signature, RecordDeclaration record) {
    for (ConstructorDeclaration constructor : record.getConstructors()) {
      if (!constructor.isImplicit() && signature.size() < constructor.getSignatureTypes().size()) {
        List<Type> workingSignature =
            getCallSignatureWithDefaults(constructExpression, constructor);
        if (constructor.hasSignature(workingSignature)) {
          for (Expression argument :
              constructor
                  .getDefaultParameters()
                  .subList(signature.size(), constructor.getDefaultParameterSignature().size())) {
            constructExpression.addArgument(argument, true);
          }
          return constructor;
        }
      }
    }
    return null;
  }

  private ConstructorDeclaration resolveConstructorWithImplicitCast(
      ConstructExpression constructExpression, RecordDeclaration recordDeclaration) {
    for (ConstructorDeclaration constructorDeclaration : recordDeclaration.getConstructors()) {
      List<Type> workingSignature = new ArrayList<>(constructExpression.getSignature());
      workingSignature.addAll(
          constructorDeclaration
              .getDefaultParameterSignature()
              .subList(
                  constructExpression.getArguments().size(),
                  constructorDeclaration.getDefaultParameterSignature().size()));
      if (compatibleSignatures(
          constructExpression.getSignature(), constructorDeclaration.getSignatureTypes())) {
        List<CastExpression> implicitCasts =
            signatureWithImplicitCastTransformation(
                getCallSignatureWithDefaults(constructExpression, constructorDeclaration),
                constructExpression.getArguments(),
                constructorDeclaration.getSignatureTypes());
        applyImplicitCastToArguments(constructExpression, implicitCasts);
        return constructorDeclaration;
      } else if (compatibleSignatures(
          workingSignature, constructorDeclaration.getSignatureTypes())) {
        List<CastExpression> implicitCasts =
            signatureWithImplicitCastTransformation(
                constructExpression.getSignature(),
                constructExpression.getArguments(),
                constructorDeclaration.getSignatureTypes());
        applyImplicitCastToArguments(constructExpression, implicitCasts);
        addDefaultArgsToCall(constructorDeclaration, constructExpression);
        return constructorDeclaration;
      }
    }
    return null;
  }

  @NonNull
  private ConstructorDeclaration getConstructorDeclarationCXX(
      ConstructExpression constructExpression, RecordDeclaration record) {
    List<Type> signature = constructExpression.getSignature();
    ConstructorDeclaration constructorCandidate =
        getConstructorDeclarationDirectMatch(signature, record);
    if (constructorCandidate == null && this.getLang() instanceof CXXLanguageFrontend) {
      // Check for usage of default args
      constructorCandidate = resolveConstructorWithDefaults(constructExpression, signature, record);
    }

    if (constructorCandidate == null && this.getLang() instanceof CXXLanguageFrontend) {
      // If we don't find any candidate and our current language is c/c++ we check if there is a
      // candidate with an implicit cast
      resolveConstructorWithImplicitCast(constructExpression, record);
    }

    if (constructorCandidate == null) {
      // Create Dummy
      constructorCandidate = createConstructorDummy(record, signature);
    }

    return constructorCandidate;
  }

  @NonNull
  private ConstructorDeclaration getConstructorDeclarationForExplicitInvocation(
      List<Type> signature, RecordDeclaration record) {
    return record.getConstructors().stream()
        .filter(f -> f.hasSignature(signature))
        .findFirst()
        .orElseGet(() -> createConstructorDummy(record, signature));
  }
}
