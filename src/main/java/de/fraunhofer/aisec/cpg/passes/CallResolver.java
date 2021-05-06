/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.*;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
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
    } else if (node instanceof ConstructExpression) {
      ConstructExpression constructExpression = (ConstructExpression) node;
      // We might have call expressions inside our arguments, so in order to correctly resolve
      // this call's signature, we need to make sure any call expression arguments are fully
      // resolved
      resolveArguments(constructExpression, curClass);
      resolveConstructExpression((ConstructExpression) node);
    } else if (node instanceof CallExpression) {
      CallExpression call = (CallExpression) node;
      // We might have call expressions inside our arguments, so in order to correctly resolve
      // this call's signature, we need to make sure any call expression arguments are fully
      // resolved
      resolveArguments(call, curClass);
      handleCallExpression(curClass, call);
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

    if (call instanceof TemplateCallExpression && lang instanceof CXXLanguageFrontend) {
      handleTemplateFunctionCalls(curClass, (TemplateCallExpression) call);
      return;
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

  private boolean isInstantiated(Node callParameter, Declaration functionParameter) {
    if (callParameter instanceof Type
        && functionParameter instanceof TypeParamDeclaration) {
      return ((TypeParamDeclaration) functionParameter)
          .canBeInstantiated((Type) callParameter);
    } else if (callParameter instanceof Expression
        && functionParameter instanceof NonTypeTemplateParamDeclaration) {
      return ((NonTypeTemplateParamDeclaration) functionParameter)
          .canBeInstantiated((Expression) callParameter);
    }
    return false;
  }

  private Map<ParameterizedType, TypeParamDeclaration>
      getParameterizedSignaturesFromInitialization(Map<Declaration, Node> intialization) {
    Map<ParameterizedType, TypeParamDeclaration> parameterizedSignature = new HashMap<>();
    for (Declaration templateParam : intialization.keySet()) {
      if (templateParam instanceof TypeParamDeclaration) {
        parameterizedSignature.put(
            (ParameterizedType) ((TypeParamDeclaration) templateParam).getType(),
            (TypeParamDeclaration) templateParam);
      }
    }
    return parameterizedSignature;
  }

  private Map<Declaration, Node> constructTemplateInitializationSignatureFromTemplateParameters(
      FunctionTemplateDeclaration functionTemplateDeclaration,
      TemplateCallExpression templateCall,
      Map<Node, TemplateDeclaration.TemplateInitialization> instantiationType,
      Map<Declaration, Integer> orderedInitializationSignature) {
    Map<Declaration, Node> instantiationSignature = new HashMap<>();
    for (int i = 0; i < functionTemplateDeclaration.getParameters().size(); i++) {
      if (i < templateCall.getTemplateParameters().size()) {
        Node callParameter = templateCall.getTemplateParameters().get(i);
        Declaration templateParameter = functionTemplateDeclaration.getParameters().get(i);
        if (isInstantiated(callParameter, templateParameter)) {
          instantiationSignature.put(templateParameter, callParameter);
          instantiationType.put(callParameter, TemplateDeclaration.TemplateInitialization.EXPLICIT);
          orderedInitializationSignature.put(templateParameter, i);
        } else {
          // If both parameters do not match, we cannot instantiate the template
          return null;
        }
      } else {
        if (((TemplateParameter) functionTemplateDeclaration.getParameters().get(i)).getDefault()
            != null) {
          // If we have a default we fill it in
          Node defaultNode =
              ((TemplateParameter) functionTemplateDeclaration.getParameters().get(i)).getDefault();
          instantiationSignature.put(
              functionTemplateDeclaration.getParameters().get(i), defaultNode);
          instantiationType.put(defaultNode, TemplateDeclaration.TemplateInitialization.DEFAULT);
          orderedInitializationSignature.put(functionTemplateDeclaration.getParameters().get(i), i);
        } else {
          // If there is no default, we don't have information on the parameter -> check
          // autodeduction
          instantiationSignature.put(functionTemplateDeclaration.getParameters().get(i), null);
          instantiationType.put(null, TemplateDeclaration.TemplateInitialization.UNKNOWN);
          orderedInitializationSignature.put(functionTemplateDeclaration.getParameters().get(i), i);
        }
      }
    }
    return instantiationSignature;
  }

  private Map<Declaration, Node> getTemplateInitializationSignature(
      FunctionTemplateDeclaration functionTemplateDeclaration,
      TemplateCallExpression templateCall,
      Map<Node, TemplateDeclaration.TemplateInitialization> instantiationType,
      Map<Declaration, Integer> orderedInitializationSignature) {
    // Construct Signature
    Map<Declaration, Node> signature =
        constructTemplateInitializationSignatureFromTemplateParameters(
            functionTemplateDeclaration,
            templateCall,
            instantiationType,
            orderedInitializationSignature);
    if (signature == null) return null;

    Map<ParameterizedType, TypeParamDeclaration> parameterizedTypeResolution =
        getParameterizedSignaturesFromInitialization(signature);

    // Check for unresolved Parameters and try to deduce Type by looking at call arguments
    for (int i = 0; i < templateCall.getArguments().size(); i++) {
      FunctionDeclaration functionDeclaration = functionTemplateDeclaration.getRealization().get(0);
      Type currentArgumentType = functionDeclaration.getParameters().get(i).getType();
      Type deductedType = templateCall.getArguments().get(i).getType();

      if (currentArgumentType instanceof ParameterizedType
          && (signature.get(parameterizedTypeResolution.get(currentArgumentType)) == null
              || instantiationType
                  .get(signature.get(parameterizedTypeResolution.get(currentArgumentType)))
                  .equals(TemplateDeclaration.TemplateInitialization.DEFAULT))) {
        signature.put(parameterizedTypeResolution.get(currentArgumentType), deductedType);
        instantiationType.put(
            deductedType, TemplateDeclaration.TemplateInitialization.AUTO_DEDUCTION);
      }
    }
    return signature;
  }

  private void handleTemplateFunctionCalls(
      RecordDeclaration curClass, TemplateCallExpression templateCall) {

    List<FunctionTemplateDeclaration> instantiationCandidates =
        lang.getScopeManager().resolveFunctionTemplateDeclaration(templateCall);

    List<FunctionTemplateDeclaration> invokes = new ArrayList<>();
    Map<FunctionTemplateDeclaration, Map<Declaration, Node>> initialization = new HashMap<>();

    for (FunctionTemplateDeclaration functionTemplateDeclaration : instantiationCandidates) {
      Map<Node, TemplateDeclaration.TemplateInitialization> initializationType = new HashMap<>();
      Map<Declaration, Integer> orderedInitializationSignature = new HashMap<>();
      if (templateCall.getTemplateParameters().size()
              <= functionTemplateDeclaration.getParameters().size()
          && templateCall.getArguments().size()
              <= functionTemplateDeclaration.getRealization().get(0).getParameters().size()) {

        Map<Declaration, Node> initializationSignature =
            getTemplateInitializationSignature(
                functionTemplateDeclaration,
                templateCall,
                initializationType,
                orderedInitializationSignature);
        FunctionDeclaration function = functionTemplateDeclaration.getRealization().get(0);

        if (initializationSignature != null
            && checkArgumentValidity(
                function,
                getCallSignature(
                    function,
                    getParameterizedSignaturesFromInitialization(initializationSignature),
                    initializationSignature),
                templateCall)) {
          // Valid Target -> Apply invocation
          applyTemplateInstantiation(
              templateCall,
              functionTemplateDeclaration,
              function,
              initializationSignature,
              initializationType,
              orderedInitializationSignature);
          return;
        }
      }
    }

    // TODO CPS Create Dummy

  }

  private void applyTemplateInstantiation(
      TemplateCallExpression templateCall,
      FunctionTemplateDeclaration functionTemplateDeclaration,
      FunctionDeclaration function,
      Map<Declaration, Node> initializationSignature,
      Map<Node, TemplateDeclaration.TemplateInitialization> initializationType,
      Map<Declaration, Integer> orderedInitializationSignature) {

    List<Node> templateInstantiationParameters =
        new ArrayList<>(orderedInitializationSignature.keySet());
    for (Map.Entry<Declaration, Integer> entry : orderedInitializationSignature.entrySet()) {

      templateInstantiationParameters.set(
          entry.getValue(), initializationSignature.get(entry.getKey()));
    }

    templateCall.setInstantiation(functionTemplateDeclaration);
    templateCall.setInvokes(List.of(function));

    // Set return Value of call if resolved
    Type returnType = function.getType();
    Map<ParameterizedType, TypeParamDeclaration> parameterizedTypeResolution =
        getParameterizedSignaturesFromInitialization(initializationSignature);
    if (function.getType() instanceof ParameterizedType) {
      returnType =
          (Type)
              initializationSignature.get(
                  parameterizedTypeResolution.get((ParameterizedType) returnType));
    }
    templateCall.setType(returnType);

    templateCall.updateTemplateParameters(initializationType, templateInstantiationParameters);

    // Apply changes to the call signature
    List<Type> templateFunctionSignature =
        getCallSignature(function, parameterizedTypeResolution, initializationSignature);
    List<Type> templateCallSignature = templateCall.getSignature();
    List<CastExpression> callSignatureImplicit =
        signatureWithImplicitCastTransformation(
            templateCallSignature, templateCall.getArguments(), templateFunctionSignature);

    for (int i = 0; i < callSignatureImplicit.size(); i++) {
      CastExpression cast = callSignatureImplicit.get(i);
      if (cast != null) {
        templateCall.setArgument(i, cast);
      }
    }

    // Add possible initializations to TemplateParamVariableDeclarations. Do this as a last step,
    // otherwise the initializationSignature Map stops working
    for (Declaration declaration : initializationSignature.keySet()) {
      if (declaration instanceof TypeParamDeclaration) {
        ((TypeParamDeclaration) declaration)
            .addPossibleInitialization((Type) initializationSignature.get(declaration));
      } else if (declaration instanceof NonTypeTemplateParamDeclaration) {
        ((NonTypeTemplateParamDeclaration) declaration)
            .addPossibleInitialization((Expression) initializationSignature.get(declaration));
      }
    }
  }

  private boolean checkArgumentValidity(
      FunctionDeclaration functionDeclaration,
      List<Type> functionDeclarationSignature,
      TemplateCallExpression templateCallExpression) {
    if (templateCallExpression.getArguments().size()
        <= functionDeclaration.getParameters().size()) {
      List<Expression> callArguments =
          new ArrayList<>(templateCallExpression.getArguments()); // Use provided arguments
      callArguments.addAll(
          functionDeclaration
              .getDefaultParameters()
              .subList(
                  callArguments.size(),
                  functionDeclaration.getDefaultParameters().size())); // Extend by defaults
      for (int i = 0; i < callArguments.size(); i++) {
        Expression callArgument = callArguments.get(i);
        if (callArgument == null
            || (!(callArgument.getType().equals(functionDeclarationSignature.get(i)))
                && (!callArgument.getType().isPrimitive()
                    || !functionDeclarationSignature.get(i).isPrimitive()))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private List<Type> getCallSignature(
      FunctionDeclaration function,
      Map<ParameterizedType, TypeParamDeclaration> parameterizedTypeResolution,
      Map<Declaration, Node> initializationSignature) {
    List<Type> templateCallSignature = new ArrayList<>();
    for (ParamVariableDeclaration argument : function.getParameters()) {
      if (argument.getType() instanceof ParameterizedType) {
        templateCallSignature.add(
            (Type)
                initializationSignature.get(parameterizedTypeResolution.get(argument.getType())));
      } else {
        templateCallSignature.add(argument.getType());
      }
    }
    return templateCallSignature;
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
   * modifies: call arguments by applying implicit casts
   *
   * @param call we want to find invocation targets for by performing implicit casts
   * @return list of invocation candidates by applying implicit casts
   */
  private List<FunctionDeclaration> resolveWithImplicitCast(
      CallExpression call, List<FunctionDeclaration> initialInvocationCandidates) {

    // Output list for invocationTargets obtaining a valid signature by performing implicit casts
    List<FunctionDeclaration> invocationTargetsWithImplicitCast = new ArrayList<>();
    List<FunctionDeclaration> invocationTargetsWithImplicitCastAndDefaults = new ArrayList<>();

    List<CastExpression> implicitCasts = null;
    List<Type> callSignature;

    // Iterate through all possible invocation candidates
    for (FunctionDeclaration functionDeclaration : initialInvocationCandidates) {
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

    return invocationTargetsWithImplicitCastAndDefaults;
  }

  /**
   * In C++ FunctionCalls must be declared before they are used to be valid invocation candidates.
   * Therefore we have the additional requirement of <code>definedBefore()</code>
   *
   * @param call we want to find invocation targets for by performing implicit casts
   * @return list of invocation candidates by applying implicit casts
   */
  private List<FunctionDeclaration> resolveWithImplicitCastFunc(CallExpression call) {
    assert lang != null;
    List<FunctionDeclaration> initialInvocationCandidates =
        lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call).stream()
            .filter(f -> !f.isImplicit() && definedBefore(f.getLocation(), call.getLocation()))
            .collect(Collectors.toList());
    return resolveWithImplicitCast(call, initialInvocationCandidates);
  }

  /**
   * Methods can be defined inline and then they can be used even if they are declared below.
   * Currently we cannot distinguish between inline methods and method definitions outside of the
   * class block. This will be considered when we have a language dependent call resolver
   *
   * @param call we want to find invocation targets for by performing implicit casts
   * @return list of invocation candidates by applying implicit casts
   */
  private List<FunctionDeclaration> resolveWithImplicitCastMethod(CallExpression call) {
    assert lang != null;
    List<FunctionDeclaration> initialInvocationCandidates =
        lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call).stream()
            .filter(f -> !f.isImplicit())
            .collect(Collectors.toList());

    return resolveWithImplicitCast(call, initialInvocationCandidates);
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
  private List<FunctionDeclaration> resolveWithDefaultArgs(
      CallExpression call, List<FunctionDeclaration> initialInvocationCandidates) {

    List<FunctionDeclaration> invocationCandidatesDefaultArgs = new ArrayList<>();

    for (FunctionDeclaration functionDeclaration : initialInvocationCandidates) {
      if (functionDeclaration.hasSignature(
          getCallSignatureWithDefaults(call, functionDeclaration))) {
        invocationCandidatesDefaultArgs.add(functionDeclaration);
      }
    }
    return invocationCandidatesDefaultArgs;
  }

  /**
   * In C++ FunctionCalls must be declared before they are used to be valid invocation candidates.
   * Therefore we have the additional requirement of <code>definedBefore()</code>
   *
   * @param call we want to find invocation targets for by adding the default arguments to the
   *     signature
   * @return list of invocation candidates that have matching signature when considering default
   *     arguments
   */
  private List<FunctionDeclaration> resolveWithDefaultArgsFunc(CallExpression call) {
    assert lang != null;
    List<FunctionDeclaration> invocationCandidates =
        lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call).stream()
            .filter(
                f ->
                    !f.isImplicit()
                        && definedBefore(f.getLocation(), call.getLocation())
                        && call.getSignature().size() < f.getSignatureTypes().size())
            .collect(Collectors.toList());
    return resolveWithDefaultArgs(call, invocationCandidates);
  }

  /**
   * Methods can be defined inline and then they can be used even if they are declared below.
   * Currently we cannot distinguish between inline methods and method definitions outside of the
   * class block. This will be considered when we have a language dependent call resolver
   *
   * @param call we want to find invocation targets for by adding the default arguments to the
   *     signature
   * @return list of invocation candidates that have matching signature when considering default
   *     arguments
   */
  private List<FunctionDeclaration> resolveWithDefaultArgsMethod(CallExpression call) {
    assert lang != null;
    List<FunctionDeclaration> invocationCandidates =
        lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call).stream()
            .filter(
                f -> !f.isImplicit() && call.getSignature().size() < f.getSignatureTypes().size())
            .collect(Collectors.toList());

    return resolveWithDefaultArgs(call, invocationCandidates);
  }

  /**
   * Checks if a declaration is located before the usage
   *
   * @param declaration
   * @param usage
   * @return false if the declaration is below the usage, true if the declaration is above the usage
   *     or there are no locations (cannot compare)
   */
  private boolean definedBefore(
      @Nullable PhysicalLocation declaration, @Nullable PhysicalLocation usage) {
    if (declaration == null || usage == null) {
      return true; // No comparison possible -> return default value
    }
    if (declaration.getArtifactLocation().equals(usage.getArtifactLocation())) {
      return usage.getRegion().compareTo(declaration.getRegion()) > 0;
    }
    return true;
  }

  private void handleNormalCalls(RecordDeclaration curClass, CallExpression call) {
    if (curClass == null && lang != null) {
      // Handle function (not method) calls
      // C++ allows function overloading. Make sure we have at least the same number of arguments
      List<FunctionDeclaration> invocationCandidates = null;
      if (this.getLang() instanceof CXXLanguageFrontend) {
        invocationCandidates =
            lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call).stream()
                .filter(
                    f ->
                        f.hasSignature(call.getSignature())
                            && definedBefore(f.getLocation(), call.getLocation()))
                .collect(Collectors.toList());

        if (invocationCandidates.isEmpty()) {
          // Check for usage of default args
          invocationCandidates.addAll(resolveWithDefaultArgsFunc(call));
        }

        if (invocationCandidates.isEmpty()) {
          // If we don't find any candidate and our current language is c/c++ we check if there is a
          // candidate with an implicit cast
          invocationCandidates.addAll(resolveWithImplicitCastFunc(call));
        }

      } else {
        invocationCandidates = lang.getScopeManager().resolveFunction(call);
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
    if (invocationCandidates.isEmpty() && lang != null) {
      if (lang instanceof CXXLanguageFrontend) {
        invocationCandidates = handleCXXMethodCall(call);

      } else {
        invocationCandidates = lang.getScopeManager().resolveFunction(call);
      }
    }

    // Find invokes by supertypes
    if (invocationCandidates.isEmpty()
        && (!(lang instanceof CXXLanguageFrontend) || shouldSearchForInvokesInParent(call))) {
      String[] nameParts = call.getName().split("\\.");
      if (nameParts.length > 0) {
        Set<RecordDeclaration> records =
            possibleContainingTypes.stream()
                .map(t -> recordMap.get(t.getTypeName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        invocationCandidates =
            getInvocationCandidatesFromParents(nameParts[nameParts.length - 1], call, records);
      }
    }

    if (curClass != null
        && !(call instanceof MemberCallExpression || call instanceof StaticCallExpression)) {
      call.setBase(curClass.getThis());
    }

    createMethodDummies(invocationCandidates, possibleContainingTypes, call);
    call.setInvokes(invocationCandidates);
  }

  /**
   * @param call
   * @return FunctionDeclarations that are invocation candidates for the MethodCall call using C++
   *     resolution techniques
   */
  private List<FunctionDeclaration> handleCXXMethodCall(CallExpression call) {
    List<FunctionDeclaration> invocationCandidates =
        lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call).stream()
            .filter(
                f ->
                    f.hasSignature(call.getSignature())
                        && definedBefore(f.getLocation(), call.getLocation()))
            .collect(Collectors.toList());

    if (invocationCandidates.isEmpty()) {
      // Check for usage of default args
      invocationCandidates.addAll(resolveWithDefaultArgsMethod(call));
    }

    if (invocationCandidates.isEmpty()) {
      // Check for usage of implicit cast
      invocationCandidates.addAll(resolveWithImplicitCastMethod(call));
    }
    return invocationCandidates;
  }

  /**
   * Creates a dummy element for each RecordDeclaration if the invocationCandidates are empty
   *
   * @param invocationCandidates
   * @param possibleContainingTypes
   * @param call
   */
  private void createMethodDummies(
      List<FunctionDeclaration> invocationCandidates,
      Set<Type> possibleContainingTypes,
      CallExpression call) {
    if (invocationCandidates.isEmpty()) {
      possibleContainingTypes.stream()
          .map(t -> recordMap.get(t.getTypeName()))
          .filter(Objects::nonNull)
          .map(r -> createDummy(r, call.getName(), call.getCode(), false, call.getSignature()))
          .forEach(invocationCandidates::add);
    }
  }

  /**
   * In C++ search we don't search in the parent if there is a potential candidate with matching
   * name
   *
   * @param call
   * @return true if we should stop searching parent, false otherwise
   */
  private boolean shouldSearchForInvokesInParent(CallExpression call) {
    return lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call).isEmpty();
  }

  private void resolveConstructExpression(ConstructExpression constructExpression) {
    String typeName = constructExpression.getType().getTypeName();
    RecordDeclaration recordDeclaration = recordMap.get(typeName);
    constructExpression.setInstantiates(recordDeclaration);

    if (recordDeclaration != null
        && recordDeclaration.getCode() != null
        && !recordDeclaration.getCode().isEmpty()) {
      ConstructorDeclaration constructor =
          getConstructorDeclaration(constructExpression, recordDeclaration);
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
      RecordDeclaration recordDeclaration = recordMap.get(eci.getContainingClass());
      List<Type> signature =
          eci.getArguments().stream().map(Expression::getType).collect(Collectors.toList());
      if (recordDeclaration != null) {
        ConstructorDeclaration constructor =
            getConstructorDeclarationForExplicitInvocation(signature, recordDeclaration);
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
    for (RecordDeclaration recordDeclaration : containingRecords) {
      MethodDeclaration dummy = NodeBuilder.newMethodDeclaration(name, "", true, recordDeclaration);
      dummy.setImplicit(true);
      List<ParamVariableDeclaration> params = Util.createParameters(call.getSignature());
      dummy.setParameters(params);
      recordDeclaration.addMethod(dummy);
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
      RecordDeclaration recordDeclaration, String name, CallExpression call) {
    List<Type> signature = call.getSignature();
    Pattern namePattern =
        Pattern.compile(
            "(" + Pattern.quote(recordDeclaration.getName()) + "\\.)?" + Pattern.quote(name));

    if (lang instanceof CXXLanguageFrontend) {
      List<FunctionDeclaration> invocationCandidate =
          new ArrayList<>(
              recordDeclaration.getMethods().stream()
                  .filter(
                      m -> namePattern.matcher(m.getName()).matches() && m.hasSignature(signature))
                  .map(FunctionDeclaration.class::cast)
                  .collect(Collectors.toList()));

      if (invocationCandidate.isEmpty()) {
        // Search for possible invocation with defaults args
        invocationCandidate.addAll(
            resolveWithDefaultArgs(
                call,
                recordDeclaration.getMethods().stream()
                    .filter(
                        m ->
                            namePattern.matcher(m.getName()).matches()
                                && !m.isImplicit()
                                && call.getSignature().size() < m.getSignatureTypes().size())
                    .map(FunctionDeclaration.class::cast)
                    .collect(Collectors.toList())));
      }

      if (invocationCandidate.isEmpty()) {
        // Search for possible invocation with implicit cast
        invocationCandidate.addAll(
            resolveWithImplicitCast(
                call,
                recordDeclaration.getMethods().stream()
                    .filter(m -> namePattern.matcher(m.getName()).matches() && !m.isImplicit())
                    .map(FunctionDeclaration.class::cast)
                    .collect(Collectors.toList())));
      }

      return invocationCandidate;
    } else {
      return recordDeclaration.getMethods().stream()
          .filter(m -> namePattern.matcher(m.getName()).matches() && m.hasSignature(signature))
          .map(FunctionDeclaration.class::cast)
          .collect(Collectors.toList());
    }
  }

  private List<FunctionDeclaration> getInvocationCandidatesFromParents(
      String name, CallExpression call, Set<RecordDeclaration> possibleTypes) {
    Set<RecordDeclaration> workingPossibleTypes = new HashSet<>(possibleTypes);
    if (possibleTypes.isEmpty()) {
      return new ArrayList<>();
    } else {
      List<FunctionDeclaration> firstLevelCandidates =
          possibleTypes.stream()
              .map(r -> getInvocationCandidatesFromRecord(r, name, call))
              .flatMap(Collection::stream)
              .collect(Collectors.toList());

      // C++ does not allow overloading at different hierarchy levels. If we find a
      // FunctionDeclaration with the same name as the function in the CallExpression we have to
      // stop the search in the parent even if the FunctionDelcaration does not match with the
      // signature of the CallExpression
      if (lang instanceof CXXLanguageFrontend) {
        workingPossibleTypes.removeIf(
            recordDeclaration -> !shouldContinueSearchInParent(recordDeclaration, name));
      }

      if (firstLevelCandidates.isEmpty() && !possibleTypes.isEmpty()) {
        return workingPossibleTypes.stream()
            .map(RecordDeclaration::getSuperTypeDeclarations)
            .map(superTypes -> getInvocationCandidatesFromParents(name, call, superTypes))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
      } else {
        return firstLevelCandidates;
      }
    }
  }

  /**
   * In C++ if there is a method that matches the name we are looking for, we have to stop searching
   * in the parents even if the signature of the method does not match
   *
   * @param recordDeclaration
   * @param name
   * @return true if there is no method in the recordDeclaration where the name of the method
   *     matches with the provided name. false otherwise
   */
  private boolean shouldContinueSearchInParent(RecordDeclaration recordDeclaration, String name) {
    Pattern namePattern =
        Pattern.compile(
            "(" + Pattern.quote(recordDeclaration.getName()) + "\\.)?" + Pattern.quote(name));

    List<FunctionDeclaration> invocationCandidate =
        new ArrayList<>(
            recordDeclaration.getMethods().stream()
                .filter(m -> namePattern.matcher(m.getName()).matches())
                .map(FunctionDeclaration.class::cast)
                .collect(Collectors.toList()));

    return invocationCandidate.isEmpty();
  }

  private Set<FunctionDeclaration> getOverridingCandidates(
      Set<Type> possibleSubTypes, FunctionDeclaration declaration) {
    return declaration.getOverriddenBy().stream()
        .filter(f -> possibleSubTypes.contains(containingType.get(f)))
        .collect(Collectors.toSet());
  }

  /**
   * @param signature of the ConstructExpression
   * @param recordDeclaration matching the class the ConstructExpression wants to construct
   * @return ConstructorDeclaration that matches the provided signature
   */
  private ConstructorDeclaration getConstructorDeclarationDirectMatch(
      List<Type> signature, RecordDeclaration recordDeclaration) {
    for (ConstructorDeclaration constructor : recordDeclaration.getConstructors()) {
      if (constructor.hasSignature(signature)) {
        return constructor;
      }
    }
    return null;
  }

  /**
   * @param constructExpression we want to find an invocation target for
   * @param signature of the ConstructExpression (without defaults)
   * @param recordDeclaration associated with the Object the ConstructExpression constructs
   * @return a ConstructDeclaration that matches with the signature of the ConstructExpression with
   *     added default arguments. The default arguments are added to the arguments edge of the
   *     ConstructExpression
   */
  private ConstructorDeclaration resolveConstructorWithDefaults(
      ConstructExpression constructExpression,
      List<Type> signature,
      RecordDeclaration recordDeclaration) {
    for (ConstructorDeclaration constructor : recordDeclaration.getConstructors()) {
      if (!constructor.isImplicit() && signature.size() < constructor.getSignatureTypes().size()) {
        List<Type> workingSignature =
            getCallSignatureWithDefaults(constructExpression, constructor);
        if (constructor.hasSignature(workingSignature)) {
          return constructor;
        }
      }
    }
    return null;
  }

  /**
   * @param constructExpression we want to find an invocation target for
   * @param recordDeclaration associated with the Object the ConstructExpression constructs
   * @return a ConstructDeclaration that matches the signature of the ConstructExpression by
   *     applying one or more implicit casts to the primitive type arguments of the
   *     ConstructExpressions. The arguments are proxied through a CastExpression to the type
   *     required by the ConstructDeclaration.
   */
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
                constructExpression.getSignature(),
                constructExpression.getArguments(),
                constructorDeclaration.getSignatureTypes());
        applyImplicitCastToArguments(constructExpression, implicitCasts);
        return constructorDeclaration;
      } else if (compatibleSignatures(
          workingSignature, constructorDeclaration.getSignatureTypes())) {
        List<CastExpression> implicitCasts =
            signatureWithImplicitCastTransformation(
                getCallSignatureWithDefaults(constructExpression, constructorDeclaration),
                constructExpression.getArguments(),
                constructorDeclaration.getSignatureTypes());
        applyImplicitCastToArguments(constructExpression, implicitCasts);

        return constructorDeclaration;
      }
    }
    return null;
  }

  /**
   * @param constructExpression we want to find an invocation target for
   * @param recordDeclaration associated with the Object the ConstructExpression constructs
   * @return a ConstructDeclaration that is an invocation of the given ConstructExpression. If there
   *     is no valid ConstructDeclaration we will create an implicit ConstructDeclaration that
   *     matches the ConstructExpression.
   */
  @NonNull
  private ConstructorDeclaration getConstructorDeclaration(
      ConstructExpression constructExpression, RecordDeclaration recordDeclaration) {
    List<Type> signature = constructExpression.getSignature();
    ConstructorDeclaration constructorCandidate =
        getConstructorDeclarationDirectMatch(signature, recordDeclaration);
    if (constructorCandidate == null && this.getLang() instanceof CXXLanguageFrontend) {
      // Check for usage of default args
      constructorCandidate =
          resolveConstructorWithDefaults(constructExpression, signature, recordDeclaration);
    }

    if (constructorCandidate == null && this.getLang() instanceof CXXLanguageFrontend) {
      // If we don't find any candidate and our current language is c/c++ we check if there is a
      // candidate with an implicit cast
      constructorCandidate =
          resolveConstructorWithImplicitCast(constructExpression, recordDeclaration);
    }

    if (constructorCandidate == null) {
      // Create Dummy
      constructorCandidate = createConstructorDummy(recordDeclaration, signature);
    }

    return constructorCandidate;
  }

  @NonNull
  private ConstructorDeclaration getConstructorDeclarationForExplicitInvocation(
      List<Type> signature, RecordDeclaration recordDeclaration) {
    return recordDeclaration.getConstructors().stream()
        .filter(f -> f.hasSignature(signature))
        .findFirst()
        .orElseGet(() -> createConstructorDummy(recordDeclaration, signature));
  }
}
