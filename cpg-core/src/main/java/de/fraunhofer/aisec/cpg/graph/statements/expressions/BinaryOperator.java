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
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.Transient;

/**
 * A binary operation expression, such as "a + b". It consists of a left hand expression (lhs), a
 * right hand expression (rhs) and an operatorCode.
 */
public class BinaryOperator extends Expression implements TypeListener {

  /** The left hand expression. */
  @SubGraph("AST")
  private Expression lhs;

  /** The right hand expression. */
  @SubGraph("AST")
  private Expression rhs;

  /** The operator code. */
  private String operatorCode;

  /** Required for compound BinaryOperators. This should not be stored in the graph */
  @Transient
  private final List<String> compoundOperators =
      List.of("*=", "/=", "%=", "+=", "-=", "<<=", ">>=", "&=", "^=", "|=");

  public Expression getLhs() {
    return lhs;
  }

  public <T extends Expression> T getLhsAs(Class<T> clazz) {
    return clazz.isInstance(this.lhs) ? clazz.cast(this.lhs) : null;
  }

  public void setLhs(Expression lhs) {
    if (this.lhs != null) {
      disconnectOldLhs();
    }
    this.lhs = lhs;
    if (lhs != null) {
      connectNewLhs(lhs);
    }
  }

  private void connectNewLhs(Expression lhs) {
    lhs.registerTypeListener(this);
    if ("=".equals(operatorCode)) {
      if (lhs instanceof DeclaredReferenceExpression) {
        // declared reference expr is the left hand side of an assignment -> writing to the var
        ((DeclaredReferenceExpression) lhs).setAccess(AccessValues.WRITE);
      } else if (lhs instanceof MemberExpression) {
        ((MemberExpression) lhs).setAccess(AccessValues.WRITE);
      }
      if (lhs instanceof TypeListener) {
        this.registerTypeListener((TypeListener) lhs);
        this.registerTypeListener((TypeListener) this.lhs);
      }
      if (this.rhs != null) {
        lhs.addPrevDFG(rhs);
      }
    } else if (compoundOperators.contains(operatorCode)) {
      if (lhs instanceof DeclaredReferenceExpression) {
        // declared reference expr is the left hand side of an assignment -> writing to the var
        ((DeclaredReferenceExpression) lhs).setAccess(AccessValues.READWRITE);
      } else if (lhs instanceof MemberExpression) {
        ((MemberExpression) lhs).setAccess(AccessValues.READWRITE);
      }
      if (lhs instanceof TypeListener) {
        this.registerTypeListener((TypeListener) lhs);
        this.registerTypeListener((TypeListener) this.lhs);
      }
      this.addPrevDFG(lhs);
      this.addNextDFG(lhs);
    } else {
      this.addPrevDFG(lhs);
    }
  }

  private void disconnectOldLhs() {
    this.lhs.unregisterTypeListener(this);
    if ("=".equals(operatorCode)) {
      if (this.lhs instanceof TypeListener) {
        unregisterTypeListener((TypeListener) this.lhs);
      }
      if (this.rhs != null) {
        this.lhs.removePrevDFG(this.lhs);
      }
      if (this.lhs instanceof TypeListener) {
        this.unregisterTypeListener((TypeListener) this.lhs);
      }
    } else if (compoundOperators.contains(operatorCode)) {
      this.removePrevDFG(this.lhs);
      this.removeNextDFG(this.lhs);
    } else {
      this.removePrevDFG(this.lhs);
    }
  }

  public Expression getRhs() {
    return rhs;
  }

  public <T extends Expression> T getRhsAs(Class<T> clazz) {
    return clazz.isInstance(this.rhs) ? clazz.cast(this.rhs) : null;
  }

  public void setRhs(Expression rhs) {
    if (this.rhs != null) {
      disconnectOldRhs();
    }
    this.rhs = rhs;
    if (rhs != null) {
      connectNewRhs(rhs);
    }
  }

  private void connectNewRhs(Expression rhs) {
    rhs.registerTypeListener(this);
    if ("=".equals(operatorCode)) {
      if (rhs instanceof TypeListener) {
        this.registerTypeListener((TypeListener) rhs);
      }
      if (this.lhs != null) {
        this.lhs.addPrevDFG(rhs);
      }
    }
    this.addPrevDFG(
        rhs); // in C++ we can have a + (b = 1) so the rhs has to connected to the BinOp in all
    // cases
  }

  private void disconnectOldRhs() {
    this.rhs.unregisterTypeListener(this);
    if ("=".equals(operatorCode)) {
      if (this.rhs instanceof TypeListener) {
        unregisterTypeListener((TypeListener) this.rhs);
      }
      if (this.lhs != null) {
        this.lhs.removePrevDFG(this.rhs);
      }
    }
    this.removePrevDFG(
        this.rhs); // in C++ we can have a + (b = 1) so the rhs has to connected to the BinOp in all
    // cases
  }

  public String getOperatorCode() {
    return operatorCode;
  }

  public void setOperatorCode(String operatorCode) {
    this.operatorCode = operatorCode;
  }

  @Override
  public void typeChanged(HasType src, Collection<HasType> root, Type oldType) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    Type previous = this.type;
    if (this.operatorCode.equals("=")) {
      setType(src.getPropagationType(), root);
    } else if (this.lhs != null && "java.lang.String".equals(this.lhs.getType().toString())
        || this.rhs != null && "java.lang.String".equals(this.rhs.getType().toString())) {
      // String + any other type results in a String
      getPossibleSubTypes().clear();
      setType(TypeParser.createFrom("java.lang.String", true), root);
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
    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.addAll(src.getPossibleSubTypes());
    setPossibleSubTypes(subTypes, root);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .append("lhs", (lhs == null ? "null" : lhs.getName()))
        .append("rhs", (rhs == null ? "null" : rhs.getName()))
        .append("operatorCode", operatorCode)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BinaryOperator)) {
      return false;
    }
    BinaryOperator that = (BinaryOperator) o;
    return super.equals(that)
        && Objects.equals(lhs, that.lhs)
        && Objects.equals(rhs, that.rhs)
        && Objects.equals(operatorCode, that.operatorCode);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
