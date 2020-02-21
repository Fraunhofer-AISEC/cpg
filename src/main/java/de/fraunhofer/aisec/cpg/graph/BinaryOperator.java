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
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;

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

  public Expression getLhs() {
    return lhs;
  }

  public <T extends Expression> T getLhsAs(Class<T> clazz) {
    return clazz.isInstance(this.lhs) ? clazz.cast(this.lhs) : null;
  }

  public void setLhs(Expression lhs) {
    if (this.lhs != null) {
      this.lhs.unregisterTypeListener(this);
      if ("=".equals(operatorCode)) {
        if (this.rhs != null) {
          this.lhs.removePrevDFG(this.lhs);
        }
        if (this.lhs instanceof TypeListener) {
          this.unregisterTypeListener((TypeListener) this.lhs);
        }
      } else {
        this.removePrevDFG(this.lhs);
      }
    }
    this.lhs = lhs;
    if (lhs != null) {
      lhs.registerTypeListener(this);
      if ("=".equals(operatorCode)) {
        if (this.rhs != null) {
          lhs.addPrevDFG(rhs);
        }
        if (lhs instanceof TypeListener) {
          this.registerTypeListener((TypeListener) this.lhs);
        }
      } else {
        this.addPrevDFG(lhs);
      }
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
      this.rhs.unregisterTypeListener(this);
      if ("=".equals(operatorCode)) {
        if (this.lhs != null) {
          this.lhs.removePrevDFG(this.rhs);
        }
      } else {
        this.removePrevDFG(this.rhs);
      }
    }
    this.rhs = rhs;
    if (rhs != null) {
      rhs.registerTypeListener(this);
      if ("=".equals(operatorCode)) {
        if (this.lhs != null) {
          this.lhs.addPrevDFG(rhs);
        }
      } else {
        this.addPrevDFG(rhs);
      }
    }
  }

  public String getOperatorCode() {
    return operatorCode;
  }

  public void setOperatorCode(String operatorCode) {
    this.operatorCode = operatorCode;
  }

  @Override
  public void typeChanged(HasType src, HasType root, Type oldType) {
    Type previous = this.type;
    if (this.operatorCode.equals("=")) {
      if (src == this.rhs) {
        setType(src.getType(), root);
      }
    } else {
      if (this.lhs != null && "java.lang.String".equals(this.lhs.getType().toString())
          || this.rhs != null && "java.lang.String".equals(this.rhs.getType().toString())) {
        getPossibleSubTypes().clear();
        setType(new Type("java.lang.String"), root);
      }
    }
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, HasType root, Set<Type> oldSubTypes) {
    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.addAll(src.getPossibleSubTypes());
    setPossibleSubTypes(subTypes, root);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("lhs", lhs)
        .append("rhs", rhs)
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
