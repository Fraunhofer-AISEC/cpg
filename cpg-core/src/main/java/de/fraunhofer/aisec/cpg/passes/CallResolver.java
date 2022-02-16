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
import de.fraunhofer.aisec.cpg.frontends.HasTemplates;
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.*;
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

  protected final Map<String, RecordDeclaration> recordMap = new HashMap<>();
  protected final List<TemplateDeclaration> templateList = new ArrayList<>();
  protected final Map<FunctionDeclaration, Type> containingType = new HashMap<>();
  @Nullable protected TranslationUnitDeclaration currentTU;
  protected ScopedWalker walker;

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
    walker.registerHandler(this::findTemplates);
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

  protected void findRecords(@NonNull Node node, RecordDeclaration curClass) {
    if (node instanceof RecordDeclaration) {
      recordMap.putIfAbsent(node.getName(), (RecordDeclaration) node);
    }
  }

  /**
   * Caches all TemplateDeclarations in {@link CallResolver#templateList}
   *
   * @param node
   * @param curClass
   */
  protected void findTemplates(@NonNull Node node, RecordDeclaration curClass) {
    if (node instanceof TemplateDeclaration) {
      templateList.add((TemplateDeclaration) node);
    }
  }

  protected void registerMethods(
      RecordDeclaration currentClass, Node parent, @NonNull Node currentNode) {
    if (currentNode instanceof MethodDeclaration && currentClass != null) {
      containingType.put(
          (FunctionDeclaration) currentNode, TypeParser.createFrom(currentClass.getName(), true));
    }
  }

  protected void fixInitializers(@NonNull Node node, RecordDeclaration curClass) {
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
          addImplicitTemplateParametersToCall(declaration.getTemplateParameters(), initializer);
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
    }
  }

  /**
   * Adds implicit duplicates of the TemplateParams to the implicit ConstructExpression
   *
   * @param templateParams of the VariableDeclaration/NewExpression
   * @param constructExpression duplicate TemplateParameters (implicit) to preserve AST, as
   *     ConstructExpression uses AST as well as the VariableDeclaration/NewExpression
   */
  public static void addImplicitTemplateParametersToCall(
      List<Node> templateParams, ConstructExpression constructExpression) {
    if (templateParams != null) {
      for (Node node : templateParams) {
        if (node instanceof TypeExpression) {
          constructExpression.addExplicitTemplateParameter(
              NodeBuilder.duplicateTypeExpression((TypeExpression) node, true));
        } else if (node instanceof Literal<?>) {
          constructExpression.addExplicitTemplateParameter(
              NodeBuilder.duplicateLiteral((Literal<?>) node, true));
        }
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
  protected void handleSuperCall(RecordDeclaration curClass, CallExpression call) {
    RecordDeclaration target = null;
    if (call.getBase().getName().equals("super")) {
      // direct superclass, either defined explicitly or java.lang.Object by default
      if (!curClass.getSuperClasses().isEmpty()) {
        target = recordMap.get(curClass.getSuperClasses().get(0).getRoot().getTypeName());
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

  protected RecordDeclaration handleSpecificSupertype(
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
          return recordMap.get(base.getSuperClasses().get(0).getRoot().getTypeName());
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

  protected void resolve(@NonNull Node node, RecordDeclaration curClass) {
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

  protected void handleCallExpression(RecordDeclaration curClass, CallExpression call) {
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

    if (call.instantiatesTemplate() && lang instanceof HasTemplates) {
      handleTemplateFunctionCalls(curClass, call, true);
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

  /**
   * Checks if the provided call parameter can instantiate the required template parameter
   *
   * @param callParameter
   * @param templateParameter
   * @return If the TemplateParameter is an TypeParamDeclaration, the callParameter must be an
   *     ObjectType =&gt; returns true If the TemplateParameter is a ParamVariableDeclaration, the
   *     callParamerter must be an Expression and its type must match the type of the
   *     ParamVariableDeclaration (same type or subtype) =&gt; returns true Otherwise return false
   */
  protected boolean isInstantiated(Node callParameter, Declaration templateParameter) {
    if (callParameter instanceof TypeExpression) {
      callParameter = ((TypeExpression) callParameter).getType();
    }

    if (callParameter instanceof Type && templateParameter instanceof TypeParamDeclaration) {
      Type type = (Type) callParameter;
      return type instanceof ObjectType;
    } else if (callParameter instanceof Expression
        && templateParameter instanceof ParamVariableDeclaration) {
      Expression expression = (Expression) callParameter;
      ParamVariableDeclaration paramVariableDeclaration =
          (ParamVariableDeclaration) templateParameter;
      return expression.getType().equals(paramVariableDeclaration.getType())
          || TypeManager.getInstance()
              .isSupertypeOf(paramVariableDeclaration.getType(), expression.getType());
    }
    return false;
  }

  /**
   * Gets all ParameterizedTypes from the initialization signature
   *
   * @param intialization mapping of the declaration of the template parameters to the explicit
   *     values the template is instantiated with
   * @return mapping of the parameterizedtypes to the corresponding TypeParamDeclaration in the
   *     template
   */
  protected Map<ParameterizedType, TypeParamDeclaration>
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

  /**
   * Check if we are handling an implicit template parameter, if so set instantiationSignature,
   * instantiationType and orderedInitializationSignature maps accordningly
   *
   * @param functionTemplateDeclaration functionTemplate we have identified
   * @param index position of the templateParameter we are currently handling
   * @param instantiationSignature mapping of the Declaration represeting a template parameter to
   *     the value that initializes that template parameter
   * @param instantiationType mapping of the instantiation value to the instantiation type (depends
   *     on resolution {@link TemplateDeclaration.TemplateInitialization}
   * @param orderedInitializationSignature mapping of the ordering of the template parameters
   */
  protected void handleImplicitTemplateParameter(
      FunctionTemplateDeclaration functionTemplateDeclaration,
      int index,
      Map<Declaration, Node> instantiationSignature,
      Map<Node, TemplateDeclaration.TemplateInitialization> instantiationType,
      Map<Declaration, Integer> orderedInitializationSignature) {
    if (((HasDefault) functionTemplateDeclaration.getParameters().get(index)).getDefault()
        != null) {
      // If we have a default we fill it in
      Node defaultNode =
          ((HasDefault) functionTemplateDeclaration.getParameters().get(index)).getDefault();

      if (defaultNode instanceof Type) {
        defaultNode = NodeBuilder.newTypeExpression(defaultNode.getName(), (Type) defaultNode);
        defaultNode.setImplicit(true);
      }

      instantiationSignature.put(
          functionTemplateDeclaration.getParameters().get(index), defaultNode);
      instantiationType.put(defaultNode, TemplateDeclaration.TemplateInitialization.DEFAULT);
      orderedInitializationSignature.put(
          functionTemplateDeclaration.getParameters().get(index), index);
    } else {
      // If there is no default, we don't have information on the parameter -> check
      // autodeduction
      instantiationSignature.put(functionTemplateDeclaration.getParameters().get(index), null);
      instantiationType.put(null, TemplateDeclaration.TemplateInitialization.UNKNOWN);
      orderedInitializationSignature.put(
          functionTemplateDeclaration.getParameters().get(index), index);
    }
  }

  /**
   * Creates a Mapping between the Parameters of the TemplateDeclaration and the Values provided for
   * the instantiation of the template (Only the ones that are in defined in the instantiation -&gt;
   * no defaults or implicit). Additionally, it fills the maps and lists mentioned below:
   *
   * @param functionTemplateDeclaration functionTemplate we have identified that should be
   *     instantiated
   * @param templateCall callExpression that instantiates the template
   * @param instantiationType mapping of the instantiation value to the instantiation type (depends
   *     * on resolution {@link TemplateDeclaration.TemplateInitialization}
   * @param orderedInitializationSignature mapping of the ordering of the template parameters
   * @param explicitInstantiated list of all ParameterizedTypes which are explicitly instantiated
   * @return mapping containing the all elements of the signature of the TemplateDeclaration as key
   *     and the Type/Expression the Parameter is initialized with. This function returns null if
   *     the {ParamVariableDeclaration, TypeParamDeclaration} do not match the provided value for
   *     initialization -&gt; initialization not possible
   */
  @Nullable
  protected Map<Declaration, Node> constructTemplateInitializationSignatureFromTemplateParameters(
      FunctionTemplateDeclaration functionTemplateDeclaration,
      CallExpression templateCall,
      Map<Node, TemplateDeclaration.TemplateInitialization> instantiationType,
      Map<Declaration, Integer> orderedInitializationSignature,
      List<ParameterizedType> explicitInstantiated) {
    Map<Declaration, Node> instantiationSignature = new HashMap<>();
    for (int i = 0; i < functionTemplateDeclaration.getParameters().size(); i++) {
      if (i < templateCall.getTemplateParameters().size()) {
        Node callParameter = templateCall.getTemplateParameters().get(i);
        Declaration templateParameter = functionTemplateDeclaration.getParameters().get(i);
        if (isInstantiated(callParameter, templateParameter)) {
          instantiationSignature.put(templateParameter, callParameter);
          instantiationType.put(callParameter, TemplateDeclaration.TemplateInitialization.EXPLICIT);
          if (templateParameter instanceof TypeParamDeclaration) {
            explicitInstantiated.add(
                (ParameterizedType) ((TypeParamDeclaration) templateParameter).getType());
          }
          orderedInitializationSignature.put(templateParameter, i);
        } else {
          // If both parameters do not match, we cannot instantiate the template
          return null;
        }
      } else {
        handleImplicitTemplateParameter(
            functionTemplateDeclaration,
            i,
            instantiationSignature,
            instantiationType,
            orderedInitializationSignature);
      }
    }
    return instantiationSignature;
  }

  /**
   * Creates a Mapping between the Parameters of the TemplateDeclaration and the Values provided *
   * for the instantiation of the template.
   *
   * <p>The difference to {@link
   * CallResolver#constructTemplateInitializationSignatureFromTemplateParameters} is that this one
   * also takes into account defaults and auto deductions
   *
   * <p>Additionally, it fills the maps and lists mentioned below:
   *
   * @param functionTemplateDeclaration functionTemplate we have identified that should be
   *     instantiated
   * @param templateCall callExpression that instantiates the template
   * @param instantiationType mapping of the instantiation value to the instantiation type (depends
   *     on resolution {@link TemplateDeclaration.TemplateInitialization}
   * @param orderedInitializationSignature mapping of the ordering of the template parameters
   * @param explicitInstantiated list of all ParameterizedTypes which are explicitly instantiated
   * @return mapping containing the all elements of the signature of the TemplateDeclaration as key
   *     and the Type/Expression the Parameter is initialized with. This function returns null if
   *     the {ParamVariableDeclaration, TypeParamDeclaration} do not match the provided value for
   *     initialization -&gt; initialization not possible
   */
  protected Map<Declaration, Node> getTemplateInitializationSignature(
      FunctionTemplateDeclaration functionTemplateDeclaration,
      CallExpression templateCall,
      Map<Node, TemplateDeclaration.TemplateInitialization> instantiationType,
      Map<Declaration, Integer> orderedInitializationSignature,
      List<ParameterizedType> explicitInstantiated) {
    // Construct Signature
    Map<Declaration, Node> signature =
        constructTemplateInitializationSignatureFromTemplateParameters(
            functionTemplateDeclaration,
            templateCall,
            instantiationType,
            orderedInitializationSignature,
            explicitInstantiated);
    if (signature == null) return null;

    Map<ParameterizedType, TypeParamDeclaration> parameterizedTypeResolution =
        getParameterizedSignaturesFromInitialization(signature);

    // Check for unresolved Parameters and try to deduce Type by looking at call arguments
    for (int i = 0; i < templateCall.getArguments().size(); i++) {
      FunctionDeclaration functionDeclaration = functionTemplateDeclaration.getRealization().get(0);
      Type currentArgumentType = functionDeclaration.getParameters().get(i).getType();
      Type deducedType = templateCall.getArguments().get(i).getType();
      TypeExpression typeExpression =
          NodeBuilder.newTypeExpression(deducedType.getName(), deducedType);
      typeExpression.setImplicit(true);

      if (currentArgumentType instanceof ParameterizedType
          && (signature.get(parameterizedTypeResolution.get(currentArgumentType)) == null
              || instantiationType
                  .get(signature.get(parameterizedTypeResolution.get(currentArgumentType)))
                  .equals(TemplateDeclaration.TemplateInitialization.DEFAULT))) {
        signature.put(parameterizedTypeResolution.get(currentArgumentType), typeExpression);
        instantiationType.put(
            typeExpression, TemplateDeclaration.TemplateInitialization.AUTO_DEDUCTION);
      }
    }
    return signature;
  }

  /**
   * @param curClass class the invoked method must be part of.
   * @param templateCall call to instantiate and invoke a function template
   * @param applyInference if the resolution was unsuccessful and applyInference is true the call
   *     will resolve to a instantiation/invocation of an inferred template
   * @return true if resolution was successful, false if not
   */
  protected boolean handleTemplateFunctionCalls(
      @Nullable RecordDeclaration curClass,
      @NonNull CallExpression templateCall,
      boolean applyInference) {
    if (lang == null) {
      Util.errorWithFileLocation(
          templateCall, log, "Could not handle template function call: language frontend is null");

      return false;
    }

    List<FunctionTemplateDeclaration> instantiationCandidates =
        lang.getScopeManager().resolveFunctionTemplateDeclaration(templateCall);

    for (FunctionTemplateDeclaration functionTemplateDeclaration : instantiationCandidates) {
      Map<Node, TemplateDeclaration.TemplateInitialization> initializationType = new HashMap<>();
      Map<Declaration, Integer> orderedInitializationSignature = new HashMap<>();
      List<ParameterizedType> explicitInstantiation = new ArrayList<>();
      if (templateCall.getTemplateParameters() != null
          && templateCall.getTemplateParameters().size()
              <= functionTemplateDeclaration.getParameters().size()
          && templateCall.getArguments().size()
              <= functionTemplateDeclaration.getRealization().get(0).getParameters().size()) {

        Map<Declaration, Node> initializationSignature =
            getTemplateInitializationSignature(
                functionTemplateDeclaration,
                templateCall,
                initializationType,
                orderedInitializationSignature,
                explicitInstantiation);
        FunctionDeclaration function = functionTemplateDeclaration.getRealization().get(0);

        if (initializationSignature != null
            && checkArgumentValidity(
                function,
                getCallSignature(
                    function,
                    getParameterizedSignaturesFromInitialization(initializationSignature),
                    initializationSignature),
                templateCall,
                explicitInstantiation)) {
          // Valid Target -> Apply invocation
          applyTemplateInstantiation(
              templateCall,
              functionTemplateDeclaration,
              function,
              initializationSignature,
              initializationType,
              orderedInitializationSignature);
          return true;
        }
      }
    }

    if (applyInference) {
      // If we want to use an inferred functionTemplateDeclaration, this needs to be provided.
      // Otherwise we could not resolve to a template and no modifications are made
      FunctionTemplateDeclaration functionTemplateDeclaration =
          createInferredFunctionTemplate(curClass, templateCall);
      templateCall.setTemplateInstantiation(functionTemplateDeclaration);
      templateCall.setInvokes(functionTemplateDeclaration.getRealization());
      // Set instantiation propertyEdges
      for (PropertyEdge<Node> instantiationParameter :
          templateCall.getTemplateParametersPropertyEdge()) {
        instantiationParameter.addProperty(
            Properties.INSTANTIATION, TemplateDeclaration.TemplateInitialization.EXPLICIT);
      }
      return true;
    }
    return false;
  }

  /**
   * Performs all necessary steps to make a CallExpression instantiate a template: 1. Set
   * TemplateInstantiation Edge from CallExpression to Template 2. Set Invokes Edge to all
   * realizations of the Tempalte 3. Set return type of the CallExpression and checks if it uses a
   * ParameterizedType and therefore has to be instantiated 4. Set Template Parameters Edge from the
   * CallExpression to all Instantiation Values 5. Set DFG Edges from instantiation to
   * ParamVariableDeclaration in TemplateDeclaration
   *
   * @param templateCall call to instantiate and invoke a function template
   * @param functionTemplateDeclaration functionTemplate we have identified that should be
   *     instantiated
   * @param function FunctionDeclaration representing the realization of the template
   * @param initializationSignature mapping containing the all elements of the signature of the
   *     TemplateDeclaration as key and the Type/Expression the Parameter is initialized with.
   * @param initializationType mapping of the instantiation value to the instantiation type (depends
   *     on resolution {@link TemplateDeclaration.TemplateInitialization}
   * @param orderedInitializationSignature mapping of the ordering of the template parameters
   */
  protected void applyTemplateInstantiation(
      CallExpression templateCall,
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

    templateCall.setTemplateInstantiation(functionTemplateDeclaration);
    templateCall.setInvokes(List.of(function));

    // Set return Value of call if resolved
    Type returnType = function.getType();
    Map<ParameterizedType, TypeParamDeclaration> parameterizedTypeResolution =
        getParameterizedSignaturesFromInitialization(initializationSignature);
    if (returnType instanceof ParameterizedType) {
      returnType =
          ((TypeExpression)
                  initializationSignature.get(parameterizedTypeResolution.get(returnType)))
              .getType();
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

    // Add DFG edges from the instantiation Expression to the ParamVariableDeclaration in the
    // Template.
    for (Map.Entry<Declaration, Node> entry : initializationSignature.entrySet()) {
      Declaration declaration = entry.getKey();
      if (declaration instanceof ParamVariableDeclaration) {
        declaration.addPrevDFG(initializationSignature.get(declaration));
        initializationSignature.get(declaration).addNextDFG(declaration);
      }
    }
  }

  /**
   * @param functionDeclaration FunctionDeclaration realization of the template
   * @param functionDeclarationSignature Signature of the realization FunctionDeclaration, but
   *     replacing the ParameterizedTypes with the ones provided in the instantiation
   * @param templateCallExpression CallExpression that instantiates the template
   * @param explicitInstantiation list of the explicitly instantiated type paramters
   * @return true if the instantiation of the template is compatible with the templatedeclaration,
   *     false otherwise
   */
  protected boolean checkArgumentValidity(
      FunctionDeclaration functionDeclaration,
      List<Type> functionDeclarationSignature,
      CallExpression templateCallExpression,
      List<ParameterizedType> explicitInstantiation) {
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
        if (callArgument == null) return false;

        if (!(callArgument.getType().equals(functionDeclarationSignature.get(i)))
            && !(callArgument.getType().isPrimitive()
                && functionDeclarationSignature.get(i).isPrimitive()
                && explicitInstantiation.contains(
                    functionDeclaration.getParameters().get(i).getType()))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * @param function FunctionDeclaration realization of the template
   * @param parameterizedTypeResolution mapping of ParameterizedTypes to the TypeParamDeclarations
   *     that define them, used to backwards resolve
   * @param initializationSignature mapping between the ParamDeclaration of the template and the
   *     corresponding instantiations
   * @return List of Types representing the Signature of the FunctionDeclaration, but
   *     ParameterizedTypes (which depend on the specific instantiation of the template) are
   *     resolved to the values the Template is instantiated with.
   */
  protected List<Type> getCallSignature(
      FunctionDeclaration function,
      Map<ParameterizedType, TypeParamDeclaration> parameterizedTypeResolution,
      Map<Declaration, Node> initializationSignature) {
    List<Type> templateCallSignature = new ArrayList<>();
    for (ParamVariableDeclaration argument : function.getParameters()) {
      if (argument.getType() instanceof ParameterizedType) {
        templateCallSignature.add(
            ((TypeExpression)
                    initializationSignature.get(
                        parameterizedTypeResolution.get(argument.getType())))
                .getType());
      } else {
        templateCallSignature.add(argument.getType());
      }
    }
    return templateCallSignature;
  }

  protected void resolveArguments(CallExpression call, RecordDeclaration curClass) {
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
  protected boolean compatibleSignatures(List<Type> callSignature, List<Type> functionSignature) {
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
  protected List<CastExpression> signatureWithImplicitCastTransformation(
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
  protected List<Type> getCallSignatureWithDefaults(
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
  protected List<FunctionDeclaration> resolveWithImplicitCast(
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
   * @param call we want to find invocation targets for by performing implicit casts
   * @return list of invocation candidates by applying implicit casts
   */
  protected List<FunctionDeclaration> resolveWithImplicitCastFunc(CallExpression call) {
    if (lang == null) {
      Util.errorWithFileLocation(
          call, log, "Could not resolve implicit casts: language frontend is null");

      return Collections.emptyList();
    }

    List<FunctionDeclaration> initialInvocationCandidates =
        new ArrayList<>(lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call));
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
  protected void checkMostCommonImplicitCast(
      List<CastExpression> implicitCasts, List<CastExpression> implicitCastTargets) {
    for (int i = 0; i < implicitCasts.size(); i++) {
      CastExpression currentCast = implicitCasts.get(i);

      if (i < implicitCastTargets.size()) {
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
  }

  /**
   * Changes the arguments of the CallExpression to use the implcit casts instead
   *
   * @param call CallExpression
   * @param implicitCasts Casts
   */
  protected void applyImplicitCastToArguments(
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
  protected void applyImplicitCastToArguments(
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
  protected List<FunctionDeclaration> resolveWithDefaultArgs(
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
   * @param call we want to find invocation targets for by adding the default arguments to the
   *     signature
   * @return list of invocation candidates that have matching signature when considering default
   *     arguments
   */
  protected List<FunctionDeclaration> resolveWithDefaultArgsFunc(CallExpression call) {
    if (lang == null) {
      Util.errorWithFileLocation(
          call, log, "Could not resolve default arguments: language frontend is null");

      return Collections.emptyList();
    }

    List<FunctionDeclaration> invocationCandidates =
        lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call).stream()
            .filter(
                f -> /*!f.isImplicit() &&*/
                    call.getSignature().size() < f.getSignatureTypes().size())
            .collect(Collectors.toList());
    return resolveWithDefaultArgs(call, invocationCandidates);
  }

  protected void handleNormalCalls(RecordDeclaration curClass, CallExpression call) {
    if (curClass == null && lang != null) {
      // Handle function (not method) calls
      // C++ allows function overloading. Make sure we have at least the same number of arguments
      List<FunctionDeclaration> invocationCandidates = null;
      if (this.getLang() instanceof CXXLanguageFrontend) {
        // Handle CXX normal call resolution externally, otherwise it leads to increased complexity
        handleNormalCallCXX(curClass, call);
      } else {
        invocationCandidates = lang.getScopeManager().resolveFunction(call);

        createInferredFunction(invocationCandidates, call);
        call.setInvokes(invocationCandidates);
      }

    } else if (!handlePossibleStaticImport(call, curClass)) {
      handleMethodCall(curClass, call);
    }
  }

  protected void handleNormalCallCXX(RecordDeclaration curClass, CallExpression call) {
    if (lang == null) {
      Util.errorWithFileLocation(
          call, log, "Could not handle normal CXX calls: language frontend is null");

      return;
    }

    List<FunctionDeclaration> invocationCandidates =
        lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call).stream()
            .filter(f -> f.hasSignature(call.getSignature()))
            .collect(Collectors.toList());

    if (invocationCandidates.isEmpty()) {
      // Check for usage of default args
      invocationCandidates.addAll(resolveWithDefaultArgsFunc(call));
    }

    if (invocationCandidates.isEmpty()) {
      /*
       Check if the call can be resolved to a function template instantiation. If it can be resolver, we
       resolve the call. Otherwise there won't be an inferred template, we will do an inferred
       FunctionDeclaration instead.
      */
      call.setTemplateParameters(new ArrayList<>());
      if (handleTemplateFunctionCalls(curClass, call, false)) {
        return;
      } else {
        call.setTemplateParameters(null);
      }
    }

    if (invocationCandidates.isEmpty()) {
      // If we don't find any candidate and our current language is c/c++ we check if there is a
      // candidate with an implicit cast
      invocationCandidates.addAll(resolveWithImplicitCastFunc(call));
    }
    createInferredFunction(invocationCandidates, call);
    call.setInvokes(invocationCandidates);
  }

  protected void createInferredFunction(
      List<FunctionDeclaration> invocationCandidates, CallExpression call) {
    if (invocationCandidates.isEmpty()) {
      // If we still have no candidates and our current language is c++ we create an inferred
      // FunctionDeclaration
      invocationCandidates.add(
          createInferredFunctionDeclaration(
              null, call.getName(), call.getCode(), false, call.getSignature()));
    }
  }

  protected void handleMethodCall(RecordDeclaration curClass, CallExpression call) {
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
        invocationCandidates = handleCXXMethodCall(curClass, call);

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
                .map(t -> recordMap.get(t.getRoot().getTypeName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        invocationCandidates =
            getInvocationCandidatesFromParents(nameParts[nameParts.length - 1], call, records);
      }
    }

    createMethodDummies(invocationCandidates, possibleContainingTypes, call);
    call.setInvokes(invocationCandidates);
  }

  /**
   * @param call
   * @return FunctionDeclarations that are invocation candidates for the MethodCall call using C++
   *     resolution techniques
   */
  protected List<FunctionDeclaration> handleCXXMethodCall(
      RecordDeclaration curClass, CallExpression call) {
    if (lang == null) {
      Util.errorWithFileLocation(
          call, log, "Could not handle method CXX calls: language frontend is null");

      return Collections.emptyList();
    }

    List<FunctionDeclaration> invocationCandidates =
        lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call).stream()
            .filter(f -> f.hasSignature(call.getSignature()))
            .collect(Collectors.toList());

    if (invocationCandidates.isEmpty()) {
      // Check for usage of default args
      invocationCandidates.addAll(resolveWithDefaultArgsFunc(call));
    }

    if (invocationCandidates.isEmpty()) {
      if (handleTemplateFunctionCalls(curClass, call, false)) {
        return call.getInvokes();
      } else {
        call.setTemplateParameters(null);
      }
    }

    if (invocationCandidates.isEmpty()) {
      // Check for usage of implicit cast
      invocationCandidates.addAll(resolveWithImplicitCastFunc(call));
    }
    return invocationCandidates;
  }

  /**
   * Creates an inferred element for each RecordDeclaration if the invocationCandidates are empty
   *
   * @param invocationCandidates
   * @param possibleContainingTypes
   * @param call
   */
  protected void createMethodDummies(
      List<FunctionDeclaration> invocationCandidates,
      Set<Type> possibleContainingTypes,
      CallExpression call) {
    if (invocationCandidates.isEmpty()) {
      possibleContainingTypes.stream()
          .map(t -> recordMap.get(t.getRoot().getTypeName()))
          .filter(Objects::nonNull)
          .map(
              r ->
                  createInferredFunctionDeclaration(
                      r, call.getName(), call.getCode(), false, call.getSignature()))
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
  protected boolean shouldSearchForInvokesInParent(CallExpression call) {
    if (lang == null) {
      Util.errorWithFileLocation(
          call, log, "Could not search for invokes in parent: language frontend is null");

      return false;
    }

    return lang.getScopeManager().resolveFunctionStopScopeTraversalOnDefinition(call).isEmpty();
  }

  protected void resolveConstructExpression(ConstructExpression constructExpression) {
    String typeName = constructExpression.getType().getTypeName();
    RecordDeclaration recordDeclaration = recordMap.get(typeName);
    constructExpression.setInstantiates(recordDeclaration);

    for (TemplateDeclaration template : templateList) {
      if (template instanceof ClassTemplateDeclaration
          && ((ClassTemplateDeclaration) template).getRealization().contains(recordDeclaration)
          && constructExpression.getTemplateParameters() != null
          && constructExpression.getTemplateParameters().size()
              <= template.getParameters().size()) {
        int defaultDifference =
            template.getParameters().size() - constructExpression.getTemplateParameters().size();
        if (defaultDifference <= template.getParameterDefaults().size()) {
          // Check if predefined template value is used as default in next value
          addRecursiveDefaultTemplateArgs(constructExpression, (ClassTemplateDeclaration) template);

          // Add missing defaults

          List<Node> missingNewParams =
              template
                  .getParameterDefaults()
                  .subList(
                      constructExpression.getTemplateParameters().size(),
                      template.getParameterDefaults().size());
          for (Node missingParam : missingNewParams) {
            constructExpression.addTemplateParameter(
                missingParam, TemplateDeclaration.TemplateInitialization.DEFAULT);
          }

          constructExpression.setTemplateInstantiation(template);
          break;
        }
      }
    }

    if (recordDeclaration != null) {
      ConstructorDeclaration constructor =
          getConstructorDeclaration(constructExpression, recordDeclaration);
      constructExpression.setConstructor(constructor);
    }
  }

  /**
   * Adds the resolved default template arguments recursively to the templateParameter list of the
   * ConstructExpression until a fixpoint is reached e.g. template&lt;class Type1, class Type2 =
   * Type1&gt;
   *
   * @param constructExpression
   * @param template
   */
  protected void addRecursiveDefaultTemplateArgs(
      ConstructExpression constructExpression, ClassTemplateDeclaration template) {
    int templateParameters;
    do {
      // Handle Explicit Template Arguments
      templateParameters = constructExpression.getTemplateParameters().size();
      Map<Node, Node> templateParametersExplicitInitialization = new HashMap<>();
      handleExplicitTemplateParameters(
          constructExpression, template, templateParametersExplicitInitialization);

      Map<Node, Node> templateParameterRealDefaultInitialization = new HashMap<>();

      // Handle defaults of parameters
      handleDefaultTemplateParameters(template, templateParameterRealDefaultInitialization);

      // Add defaults to ConstructDeclaration
      applyMissingParams(
          template,
          constructExpression,
          templateParametersExplicitInitialization,
          templateParameterRealDefaultInitialization);

    } while (templateParameters != constructExpression.getTemplateParameters().size());
  }

  /**
   * Apply missingParameters (either explicit or defaults) to the ConstructExpression and its type
   *
   * @param template Template which is instantiated by the ConstructExpression
   * @param constructExpression
   * @param templateParametersExplicitInitialization mapping of the template parameter to the
   *     explicit instantiation
   * @param templateParameterRealDefaultInitialization mapping of template parameter to its real
   *     default (no recursive)
   */
  protected void applyMissingParams(
      ClassTemplateDeclaration template,
      ConstructExpression constructExpression,
      Map<Node, Node> templateParametersExplicitInitialization,
      Map<Node, Node> templateParameterRealDefaultInitialization) {
    List<Node> missingParams =
        template
            .getParameterDefaults()
            .subList(
                constructExpression.getTemplateParameters().size(),
                template.getParameterDefaults().size());

    for (Node missingParam : missingParams) {
      if (missingParam instanceof DeclaredReferenceExpression) {
        missingParam = ((DeclaredReferenceExpression) missingParam).getRefersTo();
      }

      if (templateParametersExplicitInitialization.containsKey(missingParam)) {
        // If default is a previously defined template argument that has been explicitely passed
        constructExpression.addTemplateParameter(
            templateParametersExplicitInitialization.get(missingParam),
            TemplateDeclaration.TemplateInitialization.DEFAULT);
        // If template argument is a type add it as a generic to the type as well
        if (templateParametersExplicitInitialization.get(missingParam) instanceof TypeExpression) {
          ((ObjectType) constructExpression.getType())
              .addGeneric(
                  ((TypeExpression) templateParametersExplicitInitialization.get(missingParam))
                      .getType());
        }
      } else if (templateParameterRealDefaultInitialization.containsKey(missingParam)) {
        // Add default of template parameter to construct declaration
        constructExpression.addTemplateParameter(
            templateParameterRealDefaultInitialization.get(missingParam),
            TemplateDeclaration.TemplateInitialization.DEFAULT);
        if (templateParametersExplicitInitialization.get(missingParam) instanceof Type) {
          ((ObjectType) constructExpression.getType())
              .addGeneric(
                  ((TypeExpression) templateParametersExplicitInitialization.get(missingParam))
                      .getType());
        }
      }
    }
  }

  /**
   * Matches declared template arguments to the explicit instantiation
   *
   * @param constructExpression containing the explicit instantiation
   * @param template containing declared template arguments
   * @param templateParametersExplicitInitialization mapping of the template parameter to the
   *     explicit instantiation
   */
  protected void handleExplicitTemplateParameters(
      ConstructExpression constructExpression,
      ClassTemplateDeclaration template,
      Map<Node, Node> templateParametersExplicitInitialization) {
    for (int i = 0; i < constructExpression.getTemplateParameters().size(); i++) {
      Node explicit = constructExpression.getTemplateParameters().get(i);
      if (template.getParameters().get(i) instanceof TypeParamDeclaration) {
        templateParametersExplicitInitialization.put(
            ((TypeParamDeclaration) template.getParameters().get(i)).getType(), explicit);
      } else if (template.getParameters().get(i) instanceof ParamVariableDeclaration) {
        templateParametersExplicitInitialization.put(template.getParameters().get(i), explicit);
      }
    }
  }

  /**
   * Matches declared template arguments to their defaults (without defaults of a previously defined
   * template argument)
   *
   * @param template containing template argumetns
   * @param templateParameterRealDefaultInitialization mapping of template parameter to its real
   *     default (no recursive)
   */
  protected void handleDefaultTemplateParameters(
      ClassTemplateDeclaration template,
      Map<Node, Node> templateParameterRealDefaultInitialization) {
    List<Type> declaredTemplateTypes = new ArrayList<>();
    List<ParamVariableDeclaration> declaredNonTypeTemplate = new ArrayList<>();
    List<Declaration> parametersWithDefaults = template.getParametersWithDefaults();

    for (Declaration declaration : template.getParameters()) {
      if (declaration instanceof TypeParamDeclaration) {
        declaredTemplateTypes.add(((TypeParamDeclaration) declaration).getType());
        if (!declaredTemplateTypes.contains(((TypeParamDeclaration) declaration).getDefault())
            && parametersWithDefaults.contains(declaration)) {
          templateParameterRealDefaultInitialization.put(
              ((TypeParamDeclaration) declaration).getType(),
              ((TypeParamDeclaration) declaration).getDefault());
        }
      } else if (declaration instanceof ParamVariableDeclaration) {
        declaredNonTypeTemplate.add((ParamVariableDeclaration) declaration);
        if (parametersWithDefaults.contains(declaration)
            && (!(((ParamVariableDeclaration) declaration).getDefault()
                    instanceof DeclaredReferenceExpression)
                || !declaredNonTypeTemplate.contains(
                    ((DeclaredReferenceExpression)
                            ((ParamVariableDeclaration) declaration).getDefault())
                        .getRefersTo()))) {
          templateParameterRealDefaultInitialization.put(
              declaration, ((ParamVariableDeclaration) declaration).getDefault());
        }
      }
    }
  }

  protected void handleFunctionPointerCall(CallExpression call, Node pointer) {
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

  protected void resolveExplicitConstructorInvocation(ExplicitConstructorInvocation eci) {
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

  protected boolean handlePossibleStaticImport(
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
        generateInferredStaticallyImportedMethods(call, name, invokes, curClass);
      } else {
        invokes.add(target);
      }

      call.setInvokes(invokes);
      return true;
    }
  }

  protected void generateInferredStaticallyImportedMethods(
      @NonNull CallExpression call,
      @NonNull String name,
      @NonNull List<FunctionDeclaration> invokes,
      RecordDeclaration curClass) {
    // We had an import for this method name, just not the correct signature. Let's just add
    // an inferred node to any class that might be affected
    if (curClass == null) {
      LOGGER.warn("Cannot generate inferred nodes for imports of a null class: {}", call);
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
      MethodDeclaration inferredMethod =
          NodeBuilder.newMethodDeclaration(name, "", true, recordDeclaration);
      inferredMethod.setInferred(true);
      List<ParamVariableDeclaration> params = Util.createInferredParameters(call.getSignature());
      inferredMethod.setParameters(params);
      recordDeclaration.addMethod(inferredMethod);
      curClass.getStaticImports().add(inferredMethod);
      invokes.add(inferredMethod);
    }
  }

  /**
   * Create an inferred FunctionTemplateDeclaration if a call to an FunctionTemplate could not be
   * resolved
   *
   * @param containingRecord
   * @param call
   * @return inferred FunctionTemplateDeclaration which can be invoked by the call
   */
  protected FunctionTemplateDeclaration createInferredFunctionTemplate(
      RecordDeclaration containingRecord, CallExpression call) {
    String name = call.getName();
    String code = call.getCode();
    FunctionTemplateDeclaration inferred = NodeBuilder.newFunctionTemplateDeclaration(name, code);
    inferred.setInferred(true);
    if (containingRecord != null) {
      containingRecord.addDeclaration(inferred);
    } else {
      if (currentTU == null) {
        LOGGER.error(
            "No current translation unit when trying to generate inferred function template {}",
            inferred.getName());
      } else {
        currentTU.addDeclaration(inferred);
      }
    }
    FunctionDeclaration inferredRealization =
        createInferredFunctionDeclaration(containingRecord, name, code, false, call.getSignature());
    inferred.addRealization(inferredRealization);

    int typeCounter = 0;
    int nonTypeCounter = 0;
    for (Node node : call.getTemplateParameters()) {
      if (node instanceof TypeExpression) {
        // Template Parameter
        String inferredTypeIdentifier = "T" + typeCounter;
        TypeParamDeclaration typeParamDeclaration =
            NodeBuilder.newTypeParamDeclaration(inferredTypeIdentifier, inferredTypeIdentifier);
        typeParamDeclaration.setInferred(true);
        ParameterizedType parameterizedType = new ParameterizedType(inferredTypeIdentifier);
        parameterizedType.setInferred(true);
        typeParamDeclaration.setType(parameterizedType);
        TypeManager.getInstance().addTypeParameter(inferred, parameterizedType);
        typeCounter++;
        inferred.addParameter(typeParamDeclaration);
      } else if (node instanceof Expression) {
        // Non-Type Template Parameter
        String inferredNonTypeIdentifier = "N" + nonTypeCounter;
        ParamVariableDeclaration paramVariableDeclaration =
            NodeBuilder.newMethodParameterIn(
                inferredNonTypeIdentifier,
                ((Expression) node).getType(),
                false,
                inferredNonTypeIdentifier);
        paramVariableDeclaration.setInferred(true);
        paramVariableDeclaration.addPrevDFG(node);
        node.addNextDFG(paramVariableDeclaration);
        nonTypeCounter++;
        inferred.addParameter(paramVariableDeclaration);
      }
    }
    return inferred;
  }

  @NonNull
  protected FunctionDeclaration createInferredFunctionDeclaration(
      RecordDeclaration containingRecord,
      String name,
      String code,
      boolean isStatic,
      List<Type> signature) {

    List<ParamVariableDeclaration> parameters = Util.createInferredParameters(signature);
    if (containingRecord != null) {
      MethodDeclaration inferred =
          NodeBuilder.newMethodDeclaration(name, code, isStatic, containingRecord);
      inferred.setInferred(true);
      inferred.setParameters(parameters);

      containingRecord.addMethod(inferred);

      // "upgrade" our struct to a class, if it was inferred by us, since we are calling methods on
      // it
      if (lang != null
          && lang.getConfig().getInferenceConfiguration().getInferRecords()
          && containingRecord.isInferred()
          && containingRecord.getKind().equals("struct")) {
        containingRecord.setKind("class");
      }

      log.debug(
          "Inferring a new method declaration {} with parameter types {}",
          inferred.getName(),
          inferred.getParameters().stream()
              .map(param -> param.getType().getName())
              .collect(Collectors.toList()));

      return inferred;
    } else {
      // function declaration, not inside a class
      FunctionDeclaration inferred = NodeBuilder.newFunctionDeclaration(name, code);
      inferred.setParameters(parameters);
      inferred.setInferred(true);
      if (currentTU == null) {
        LOGGER.error(
            "No current translation unit when trying to generate  inferred function {}",
            inferred.getName());
      } else {
        currentTU.addDeclaration(inferred);
      }
      return inferred;
    }
  }

  protected ConstructorDeclaration createInferredConstructor(
      @NonNull RecordDeclaration containingRecord, List<Type> signature) {
    ConstructorDeclaration inferred =
        NodeBuilder.newConstructorDeclaration(containingRecord.getName(), "", containingRecord);
    inferred.setInferred(true);
    inferred.setParameters(Util.createInferredParameters(signature));
    containingRecord.addConstructor(inferred);
    return inferred;
  }

  protected Set<Type> getPossibleContainingTypes(Node node, RecordDeclaration curClass) {
    Set<Type> possibleTypes = new HashSet<>();
    if (node instanceof MemberCallExpression) {
      MemberCallExpression memberCall = (MemberCallExpression) node;
      if (memberCall.getBase() instanceof HasType) {
        HasType base = memberCall.getBase();
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

  protected List<FunctionDeclaration> getInvocationCandidatesFromRecord(
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
                                /*&& !m.isImplicit()*/
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
                    .filter(m -> namePattern.matcher(m.getName()).matches() /*&& !m.isImplicit()*/)
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

  protected List<FunctionDeclaration> getInvocationCandidatesFromParents(
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
      // stop the search in the parent even if the FunctionDeclaration does not match with the
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
  protected boolean shouldContinueSearchInParent(RecordDeclaration recordDeclaration, String name) {
    Pattern namePattern =
        Pattern.compile(
            "(" + Pattern.quote(recordDeclaration.getName()) + "\\.)?" + Pattern.quote(name));

    List<FunctionDeclaration> invocationCandidate =
        recordDeclaration.getMethods().stream()
            .filter(m -> namePattern.matcher(m.getName()).matches())
            .map(FunctionDeclaration.class::cast)
            .collect(Collectors.toList());

    return invocationCandidate.isEmpty();
  }

  protected Set<FunctionDeclaration> getOverridingCandidates(
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
  protected ConstructorDeclaration getConstructorDeclarationDirectMatch(
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
  protected ConstructorDeclaration resolveConstructorWithDefaults(
      ConstructExpression constructExpression,
      List<Type> signature,
      RecordDeclaration recordDeclaration) {
    for (ConstructorDeclaration constructor : recordDeclaration.getConstructors()) {
      if (
      /*!constructor.isImplicit() &&*/ signature.size() < constructor.getSignatureTypes().size()) {
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
  protected ConstructorDeclaration resolveConstructorWithImplicitCast(
      ConstructExpression constructExpression, RecordDeclaration recordDeclaration) {
    for (ConstructorDeclaration constructorDeclaration : recordDeclaration.getConstructors()) {
      var workingSignature = new ArrayList<>(constructExpression.getSignature());
      var defaultParameterSignature = constructorDeclaration.getDefaultParameterSignature();

      if (constructExpression.getArguments().size() <= defaultParameterSignature.size()) {
        workingSignature.addAll(
            defaultParameterSignature.subList(
                constructExpression.getArguments().size(), defaultParameterSignature.size()));
      }

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
  protected ConstructorDeclaration getConstructorDeclaration(
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
      // Create inferred node
      constructorCandidate = createInferredConstructor(recordDeclaration, signature);
    }

    return constructorCandidate;
  }

  @NonNull
  protected ConstructorDeclaration getConstructorDeclarationForExplicitInvocation(
      List<Type> signature, RecordDeclaration recordDeclaration) {
    return recordDeclaration.getConstructors().stream()
        .filter(f -> f.hasSignature(signature))
        .findFirst()
        .orElseGet(() -> createInferredConstructor(recordDeclaration, signature));
  }
}
