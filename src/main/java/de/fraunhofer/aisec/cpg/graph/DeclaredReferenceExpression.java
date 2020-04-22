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
import de.fraunhofer.aisec.cpg.graph.type.Type;
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

  /** The {@link ValueDeclaration}s this expression might refer to. */
  private Set<ValueDeclaration> refersTo = new HashSet<>();

  /**
   * Is this reference used for writing data instead of just reading it? Determines dataflow
   * direction
   */
  private boolean writingAccess = false;

  public Set<ValueDeclaration> getRefersTo() {
    return refersTo;
  }

  public void setRefersTo(@NonNull ValueDeclaration refersTo) {
    HashSet<ValueDeclaration> n = new HashSet<>();
    n.add(refersTo);
    setRefersTo(n);
  }

  public void setRefersTo(@NonNull Set<ValueDeclaration> refersTo) {
    this.refersTo.forEach(
        r -> {
          if (writingAccess) {
            this.removeNextDFG(r);
          } else {
            this.removePrevDFG(r);
          }
          r.unregisterTypeListener(this);
          if (r instanceof TypeListener) {
            this.unregisterTypeListener((TypeListener) r);
          }
        });

    this.refersTo.clear();
    this.refersTo.addAll(refersTo);

    refersTo.forEach(
        r -> {
          if (writingAccess) {
            this.addNextDFG(r);
          } else {
            this.addPrevDFG(r);
          }
          r.registerTypeListener(this);
          if (r instanceof TypeListener) {
            this.registerTypeListener((TypeListener) r);
          }
        });
  }

  @Override
  public void typeChanged(HasType src, HasType root, Type oldType) {
    Type previous = this.type;
    setType(src.getPropagationType(), root);
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Type.Origin.DATAFLOW);
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
        .append("refersTo", refersTo)
        .toString();
  }

  public void setWritingAccess(boolean writingAccess) {
    this.refersTo.forEach(
        r -> {
          if (this.writingAccess) {
            this.removeNextDFG(r);
          } else {
            this.removePrevDFG(r);
          }
        });

    this.writingAccess = writingAccess;
    refersTo.forEach(
        r -> {
          if (this.writingAccess) {
            this.addNextDFG(r);
          } else {
            this.addPrevDFG(r);
          }
        });
  }

  public boolean isWritingAccess() {
    return writingAccess;
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
