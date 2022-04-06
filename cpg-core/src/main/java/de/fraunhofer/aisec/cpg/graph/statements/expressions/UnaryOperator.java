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

import de.fraunhofer.aisec.cpg.graph.AccessValues;
import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.types.PointerType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.util.*;
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

  @Transient private final Set<TypeListener> checked = new HashSet<>();

  public Expression getInput() {
    return input;
  }

  public void setInput(Expression input) {
    if (this.input != null) {
      this.input.unregisterTypeListener(this);
      this.removePrevDFG(this.input);
      this.removeNextDFG(this.input);
    }
    this.input = input;
    if (input != null) {
      input.registerTypeListener(this);
      this.addPrevDFG(input);
      if (this.operatorCode.equals("++") || this.operatorCode.equals("--")) {
        this.addNextDFG(input);
      }
      changeExpressionAccess();
    }
  }

  private void changeExpressionAccess() {
    AccessValues access = AccessValues.READ;
    if (this.operatorCode.equals("++") || this.operatorCode.equals("--")) {
      access = AccessValues.READWRITE;
    }

    if (this.input instanceof DeclaredReferenceExpression) {
      ((DeclaredReferenceExpression) this.input).setAccess(access);
    } else if (this.input instanceof MemberExpression) {
      ((MemberExpression) this.input).setAccess(access);
    }
  }

  private boolean getsDataFromInput(TypeListener curr, TypeListener target) {
    List<TypeListener> worklist = new ArrayList<>();
    worklist.add(curr);
    while (!worklist.isEmpty()) {
      TypeListener tl = worklist.remove(0);
      if (!checked.contains(tl)) {
        checked.add(tl);

        if (tl == target) {
          return true;
        }

        if (curr instanceof HasType) {
          worklist.addAll(((HasType) curr).getTypeListeners());
        }
      }
    }
    return false;
  }

  private boolean getsDataFromInput(TypeListener listener) {
    checked.clear();
    if (input == null) return false;
    for (var l : input.getTypeListeners()) {
      if (getsDataFromInput(l, listener)) return true;
    }
    return false;
  }

  public String getOperatorCode() {
    return operatorCode;
  }

  public void setOperatorCode(String operatorCode) {
    this.operatorCode = operatorCode;
    changeExpressionAccess();
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
  public void typeChanged(HasType src, Collection<HasType> root, Type oldType) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    Type previous = this.type;

    if (src == input) {
      Type newType = src.getPropagationType();

      if (operatorCode.equals("*")) {
        newType = newType.dereference();
      } else if (operatorCode.equals("&")) {
        newType = newType.reference(PointerType.PointerOrigin.POINTER);
      }

      setType(newType, root);
    } else {
      // Our input didn't change, so we don't need to (de)reference the type
      setType(src.getPropagationType(), root);

      // Pass the type on to the input in an inversely (de)referenced way
      Type newType = src.getPropagationType();
      if (operatorCode.equals("*")) {
        newType = src.getPropagationType().reference(PointerType.PointerOrigin.POINTER);
      } else if (operatorCode.equals("&")) {
        newType = src.getPropagationType().dereference();
      }

      // We are a fuzzy parser, so while this should not happen, there is no guarantee that input is
      // not null
      if (input != null) {
        input.setType(newType, new ArrayList<>(List.of(this)));
      }
    }

    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Type.Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(
      HasType src, Collection<HasType> root, Set<Type> oldSubTypes) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    if (src instanceof TypeListener && getsDataFromInput((TypeListener) src)) {
      return;
    }
    Set<Type> currSubTypes = new HashSet<>(getPossibleSubTypes());
    Set<Type> newSubTypes = src.getPossibleSubTypes();
    currSubTypes.addAll(newSubTypes);

    if (operatorCode.equals("*")) {
      currSubTypes =
          currSubTypes.stream()
              .filter(Util.distinctBy(Type::getTypeName))
              .map(Type::dereference)
              .collect(Collectors.toSet());
    } else if (operatorCode.equals("&")) {
      currSubTypes =
          currSubTypes.stream()
              .filter(Util.distinctBy(Type::getTypeName))
              .map(t -> t.reference(PointerType.PointerOrigin.POINTER))
              .collect(Collectors.toSet());
    }

    getPossibleSubTypes().clear();
    setPossibleSubTypes(currSubTypes, root); // notify about the new type
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
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
