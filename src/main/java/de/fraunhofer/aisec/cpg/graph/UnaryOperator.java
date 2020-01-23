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

package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.Type.Origin;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.Transient;

/**
 * A unary operator expression, involving one expression and an operator, such as <code>a++</code>.
 */
public class UnaryOperator extends Expression implements TypeListener {

  public static final String OPERATOR_POSTFIX_INCREMENT = "++";
  public static final String OPERATOR_POSTFIX_DECREMENT = "--";

  /** The expression on which the operation is applied. */
  @SubGraph("AST")
  private Expression input;

  /** The operator code. */
  private String operatorCode;

  /** Specifies, whether this a post fix operation. */
  private boolean postfix;

  /** Specifies, whether this a pre fix operation. */
  private boolean prefix;

  @Transient private Set<TypeListener> checked = new HashSet<>();

  public Expression getInput() {
    return input;
  }

  public void setInput(Expression input) {
    if (this.input != null) {
      this.input.unregisterTypeListener(this);
      this.removePrevDFG(this.input);
    }
    this.input = input;
    if (input != null) {
      input.registerTypeListener(this);
      this.addPrevDFG(input);
    }
  }

  @Override
  public boolean shouldBeNotified(TypeListener listener) {
    if (listener instanceof HasType
        && TypeManager.getInstance().isUnknown(((HasType) listener).getType())) {
      return true;
    }
    if ("&*".contains(operatorCode)) {
      checked.clear();
      return !isTransitiveTypeListenerOfTargets(listener, List.of(this, this.input));
    } else {
      return true;
    }
  }

  private boolean isTransitiveTypeListenerOfTargets(TypeListener listener, List<Object> targets) {
    if (checked.contains(listener)) {
      return false;
    }
    checked.add(listener);

    if (targets.contains(listener)) {
      return true;
    }
    if (listener instanceof HasType) {
      return ((HasType) listener)
          .getTypeListeners().stream().anyMatch(l -> isTransitiveTypeListenerOfTargets(l, targets));
    }
    return false;
  }

  public String getOperatorCode() {
    return operatorCode;
  }

  public void setOperatorCode(String operatorCode) {
    this.operatorCode = operatorCode;
  }

  public boolean isPostfix() {
    return postfix;
  }

  public void setPostfix(boolean postfix) {
    this.postfix = postfix;
  }

  public boolean isPrefix() {
    return prefix;
  }

  public void setPrefix(boolean prefix) {
    this.prefix = prefix;
  }

  @Override
  public void typeChanged(HasType src, Type oldType) {
    Type previous = this.type;
    Type newType = src.getType();

    checked.clear();
    if (src instanceof TypeListener
        && isTransitiveTypeListenerOfTargets((TypeListener) src, List.of(this, this.input))) {
      return;
    }

    if (operatorCode.equals("*")) {
      newType = newType.dereference();
    } else if (operatorCode.equals("&")) {
      newType = newType.reference();
    }

    setType(newType);

    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, Set<Type> oldSubTypes) {
    checked.clear();
    if (src instanceof TypeListener
        && isTransitiveTypeListenerOfTargets((TypeListener) src, List.of(this, this.input))) {
      return;
    }

    Set<Type> currSubTypes = new HashSet<>(getPossibleSubTypes());
    Set<Type> newSubTypes = src.getPossibleSubTypes();

    if (operatorCode.equals("*")) {
      newSubTypes = newSubTypes.stream().map(Type::dereference).collect(Collectors.toSet());
    } else if (operatorCode.equals("&")) {
      newSubTypes = newSubTypes.stream().map(Type::reference).collect(Collectors.toSet());
    }

    currSubTypes.addAll(newSubTypes);
    setPossibleSubTypes(currSubTypes);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("input", input)
        .append("operatorCode", operatorCode)
        .append("postfix", postfix)
        .append("prefix", prefix)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UnaryOperator)) {
      return false;
    }
    UnaryOperator that = (UnaryOperator) o;
    return super.equals(that)
        && postfix == that.postfix
        && prefix == that.prefix
        && Objects.equals(input, that.input)
        && Objects.equals(operatorCode, that.operatorCode);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
