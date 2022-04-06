/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.HasBase;
import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.neo4j.ogm.annotation.Relationship;

/**
 * An expression, which calls another function. It has a list of arguments (list of {@link
 * Expression}s) and is connected via the INVOKES edge to its {@link FunctionDeclaration}.
 */
public class CallExpression extends Expression
    implements TypeListener, HasBase, HasType.SecondaryTypeEdge {

  /**
   * Connection to its {@link FunctionDeclaration}. This will be populated by the {@link
   * de.fraunhofer.aisec.cpg.passes.CallResolver}.
   */
  @Relationship(value = "INVOKES", direction = "OUTGOING")
  protected List<PropertyEdge<FunctionDeclaration>> invokes = new ArrayList<>();

  /** The list of arguments. */
  @Relationship(value = "ARGUMENTS", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<Expression>> arguments = new ArrayList<>();

  /**
   * The base object. This is marked as an AST child, because this is required for {@link
   * MemberCallExpression}. Be aware that for simple calls the implicit "this" base is not part of
   * the original AST, but we treat it as such for better consistency
   */
  @SubGraph("AST")
  private Expression base;

  private String fqn;

  @NotNull
  public Expression getBase() {
    return base;
  }

  public void setBase(Expression base) {
    if (this.base != null) {
      this.base.unregisterTypeListener(this);
    }
    this.base = base;
    if (base != null) {
      base.registerTypeListener(this);
    }
  }

  @NonNull
  public List<Expression> getArguments() {
    List<Expression> targets = new ArrayList<>();
    for (PropertyEdge<Expression> propertyEdge : this.arguments) {
      targets.add(propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(targets);
  }

  public void setArgument(int index, Expression argument) {
    this.arguments.get(index).setEnd(argument);
  }

  @NonNull
  public List<PropertyEdge<Expression>> getArgumentsPropertyEdge() {
    return this.arguments;
  }

  public void addArgument(Expression expression) {
    addArgument(expression, null);
  }

  public void addArgument(Expression expression, @Nullable String name) {
    PropertyEdge<Expression> propertyEdge = new PropertyEdge<>(this, expression);
    propertyEdge.addProperty(Properties.INDEX, this.arguments.size());

    if (name != null) {
      propertyEdge.addProperty(Properties.NAME, name);
    }

    this.arguments.add(propertyEdge);
  }

  public void setArguments(List<Expression> arguments) {
    this.arguments = PropertyEdge.transformIntoOutgoingPropertyEdgeList(arguments, this);
  }

  @NonNull
  public List<FunctionDeclaration> getInvokes() {
    List<FunctionDeclaration> targets = new ArrayList<>();
    for (PropertyEdge<FunctionDeclaration> propertyEdge : this.invokes) {
      targets.add(propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(targets);
  }

  public List<PropertyEdge<FunctionDeclaration>> getInvokesPropertyEdge() {
    return this.invokes;
  }

  public void setInvokes(List<FunctionDeclaration> invokes) {
    PropertyEdge.unwrap(this.invokes)
        .forEach(
            i -> {
              i.unregisterTypeListener(this);
              Util.detachCallParameters(i, this.getArguments());
              this.removePrevDFG(i);
            });
    this.invokes = PropertyEdge.transformIntoOutgoingPropertyEdgeList(invokes, this);
    invokes.forEach(
        i -> {
          i.registerTypeListener(this);
          Util.attachCallParameters(i, this.getArguments());
          this.addPrevDFG(i);
        });
  }

  public List<Type> getSignature() {
    return getArguments().stream().map(Expression::getType).collect(Collectors.toList());
  }

  boolean template;

  public void setTemplate(boolean template) {
    this.template = template;
    if (template) {
      this.templateParameters = new ArrayList<>();
    }
  }

  /** If the CallExpression instantiates a Template, the call can provide template parameters */
  @Relationship(value = "TEMPLATE_PARAMETERS", direction = "OUTGOING")
  @SubGraph("AST")
  @Nullable
  private List<PropertyEdge<Node>> templateParameters;

  /**
   * If the CallExpression instantiates a Template the CallExpression is connected to the template
   * which is instantiated. This is required by the expansion pass to access the Template directly.
   * The invokes edge will still point to the realization of the template.
   */
  @Relationship(value = "TEMPLATE_INSTANTIATION", direction = "OUTGOING")
  @Nullable
  private TemplateDeclaration templateInstantiation;

  @Nullable
  public List<PropertyEdge<Node>> getTemplateParametersPropertyEdge() {
    return templateParameters;
  }

  @Nullable
  public List<Node> getTemplateParameters() {
    if (this.templateParameters == null) {
      return null;
    }
    return PropertyEdge.unwrap(this.templateParameters);
  }

  @Nullable
  public List<Type> getTypeTemplateParameters() {
    if (this.templateParameters == null) {
      return null;
    }
    List<Type> types = new ArrayList<>();
    for (Node n : getTemplateParameters()) {
      if (n instanceof Type) {
        types.add((Type) n);
      }
    }
    return types;
  }

  public void addTemplateParameter(
      Type typeTemplateParam, TemplateDeclaration.TemplateInitialization templateInitialization) {
    if (this.templateParameters == null) {
      this.templateParameters = new ArrayList<>();
    }
    PropertyEdge<Node> propertyEdge = new PropertyEdge<>(this, typeTemplateParam);
    propertyEdge.addProperty(Properties.INDEX, this.templateParameters.size());
    propertyEdge.addProperty(Properties.INSTANTIATION, templateInitialization);
    this.templateParameters.add(propertyEdge);
    this.template = true;
  }

  public void replaceTypeTemplateParameter(Type oldType, Type newType) {
    if (this.templateParameters == null) {
      return;
    }
    for (int i = 0; i < this.templateParameters.size(); i++) {
      PropertyEdge<Node> propertyEdge = this.templateParameters.get(i);
      if (propertyEdge.getEnd().equals(oldType)) {
        propertyEdge.setEnd(newType);
      }
    }
  }

  public void addTemplateParameter(
      Expression expressionTemplateParam,
      TemplateDeclaration.TemplateInitialization templateInitialization) {
    if (this.templateParameters == null) {
      this.templateParameters = new ArrayList<>();
    }
    PropertyEdge<Node> propertyEdge = new PropertyEdge<>(this, expressionTemplateParam);
    propertyEdge.addProperty(Properties.INDEX, this.templateParameters.size());
    propertyEdge.addProperty(Properties.INSTANTIATION, templateInitialization);
    this.templateParameters.add(propertyEdge);
    this.template = true;
  }

  public void addTemplateParameter(
      Node templateParam, TemplateDeclaration.TemplateInitialization templateInitialization) {
    if (templateParam instanceof Expression) {
      addTemplateParameter((Expression) templateParam, templateInitialization);
    } else if (templateParam instanceof Type) {
      addTemplateParameter((Type) templateParam, templateInitialization);
    }
  }

  public void addExplicitTemplateParameter(Node templateParameter) {
    addTemplateParameter(templateParameter, TemplateDeclaration.TemplateInitialization.EXPLICIT);
  }

  public void addExplicitTemplateParameters(List<Node> templateParameters) {
    for (Node node : templateParameters) {
      addTemplateParameter(node, TemplateDeclaration.TemplateInitialization.EXPLICIT);
    }
  }

  public void removeRealization(Node templateParam) {
    if (this.templateParameters == null) {
      return;
    }
    this.templateParameters.removeIf(propertyEdge -> propertyEdge.getEnd().equals(templateParam));
  }

  public void setTemplateParameters(List<PropertyEdge<Node>> templateParameters) {
    this.templateParameters = templateParameters;
    template = templateParameters != null;
  }

  @Nullable
  public TemplateDeclaration getTemplateInstantiation() {
    return templateInstantiation;
  }

  public void setTemplateInstantiation(TemplateDeclaration templateInstantiation) {
    this.templateInstantiation = templateInstantiation;
    template = templateInstantiation != null;
  }

  public void updateTemplateParameters(
      Map<Node, TemplateDeclaration.TemplateInitialization> initializationType,
      List<Node> orderedInitializationSignature) {
    if (this.templateParameters == null) {
      return;
    }
    for (PropertyEdge<Node> edge : this.templateParameters) {
      if (edge.getProperty(Properties.INSTANTIATION) != null
          && edge.getProperty(Properties.INSTANTIATION)
              .equals(TemplateDeclaration.TemplateInitialization.UNKNOWN)
          && initializationType.containsKey(edge.getEnd())) {
        edge.addProperty(Properties.INSTANTIATION, initializationType.get(edge.getEnd()));
      }
    }

    for (int i = this.templateParameters.size(); i < orderedInitializationSignature.size(); i++) {
      PropertyEdge<Node> propertyEdge =
          new PropertyEdge<>(this, orderedInitializationSignature.get(i));
      propertyEdge.addProperty(Properties.INDEX, this.templateParameters.size());
      propertyEdge.addProperty(
          Properties.INSTANTIATION,
          initializationType.getOrDefault(
              orderedInitializationSignature.get(i),
              TemplateDeclaration.TemplateInitialization.UNKNOWN));
      this.templateParameters.add(propertyEdge);
    }
  }

  public boolean instantiatesTemplate() {
    return templateInstantiation != null || templateParameters != null || template;
  }

  @Override
  public void typeChanged(HasType src, Collection<HasType> root, Type oldType) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    if (src == base) {
      setFqn(src.getType().getRoot().getTypeName() + "." + this.getName());
    } else {
      Type previous = this.type;
      List<Type> types =
          invokes.stream()
              .map(PropertyEdge::getEnd)
              .map(FunctionDeclaration::getType)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      Type alternative = !types.isEmpty() ? types.get(0) : null;
      Type commonType = TypeManager.getInstance().getCommonType(types).orElse(alternative);
      Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
      subTypes.remove(oldType);
      subTypes.addAll(types);

      setType(commonType, root);
      setPossibleSubTypes(subTypes, root);

      if (!previous.equals(this.type)) {
        this.type.setTypeOrigin(Type.Origin.DATAFLOW);
      }
    }
  }

  @Override
  public void possibleSubTypesChanged(
      HasType src, Collection<HasType> root, Set<Type> oldSubTypes) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    if (src != base) {
      Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
      subTypes.addAll(src.getPossibleSubTypes());
      setPossibleSubTypes(subTypes, root);
    }
  }

  @NotNull
  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("base", base)
        .toString();
  }

  public String getFqn() {
    return fqn;
  }

  public void setFqn(String fqn) {
    this.fqn = fqn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CallExpression)) {
      return false;
    }
    CallExpression that = (CallExpression) o;
    return super.equals(that)
        && Objects.equals(this.getArguments(), that.getArguments())
        && PropertyEdge.propertyEqualsList(arguments, that.arguments)
        && Objects.equals(this.getInvokes(), that.getInvokes())
        && PropertyEdge.propertyEqualsList(invokes, that.invokes)
        && Objects.equals(base, that.base)
        && ((templateParameters == that.templateParameters)
            || (templateParameters.equals(that.templateParameters)
                && PropertyEdge.propertyEqualsList(templateParameters, that.templateParameters)))
        && ((templateInstantiation == that.templateInstantiation)
            || (templateInstantiation.equals(that.templateInstantiation)))
        && template == that.template;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public void updateType(Collection<Type> typeState) {
    if (this.templateParameters == null) {
      return;
    }
    for (Type t : this.getTypeTemplateParameters()) {
      for (Type t2 : typeState) {
        if (t2.equals(t)) {
          this.replaceTypeTemplateParameter(t, t2);
        }
      }
    }
  }
}
