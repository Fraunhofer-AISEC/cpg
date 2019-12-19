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
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An expression, which refers to something which is declared, e.g. a variable. For example, the
 * expression <code>a = b</code>, which itself is a {@link BinaryOperator}, contains two {@link
 * DeclaredReferenceExpression}s, one for the variable <code>a</code> and one for variable <code>b
 * </code>, which have been previously been declared.
 */
public class DeclaredReferenceExpression extends Expression implements TypeListener {

  /** The {@link ValueDeclaration} this expression refers to. */
  private ValueDeclaration refersTo;

  public ValueDeclaration getRefersTo() {
    return refersTo;
  }

  public void setRefersTo(@NonNull ValueDeclaration refersTo) {
    if (this.refersTo != null) {
      this.refersTo.unregisterTypeListener(this);
      this.removePrevDFG(this.refersTo);
      this.refersTo.removePrevDFG(this);
      if (this.refersTo instanceof TypeListener) {
        this.unregisterTypeListener((TypeListener) this.refersTo);
      }
    }

    this.refersTo = refersTo;
    refersTo.registerTypeListener(this);
    this.addPrevDFG(refersTo);
    refersTo.addPrevDFG(this);
    if (refersTo instanceof TypeListener) {
      this.registerTypeListener((TypeListener) refersTo);
    }
  }

  @Override
  public void typeChanged(HasType src, Type oldType) {
    Type previous = this.type;
    setType(src.getType());
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, Set<Type> oldSubTypes) {
    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.addAll(src.getPossibleSubTypes());
    setPossibleSubTypes(subTypes);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("refersTo", refersTo)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DeclaredReferenceExpression)) {
      return false;
    }
    DeclaredReferenceExpression that = (DeclaredReferenceExpression) o;
    return super.equals(that) && Objects.equals(refersTo, that.refersTo);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
