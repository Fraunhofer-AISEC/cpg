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
 * Represents access to a field of a {@link RecordDeclaration}, such as <code>obj.property</code>.
 */
public class MemberExpression extends Expression implements TypeListener {

  @SubGraph("AST")
  protected Node member;

  @SubGraph("AST")
  @NonNull
  private Node base;

  /**
   * Is this reference used for writing data instead of just reading it? Determines dataflow
   * direction
   */
  private AccessValues access = AccessValues.READ;

  @NonNull
  public Node getBase() {
    return base;
  }

  public void setBase(@NonNull Node base) {
    this.base = base;
  }

  public Node getMember() {
    return member;
  }

  public void setMember(Node member) {
    if (this.member instanceof TypeListener) {
      unregisterTypeListener((TypeListener) this.member);
    }
    if (this.member instanceof HasType) {
      ((HasType) this.member).unregisterTypeListener(this);
    }
    if (this.access == AccessValues.WRITE) {
      this.removeNextDFG(this.member);
    } else if (this.access == AccessValues.READ) {
      this.removePrevDFG(this.member);
    } else {
      this.removeNextDFG(this.member);
      this.removePrevDFG(this.member);
    }

    this.member = member;

    if (member instanceof HasType) {
      ((HasType) member).registerTypeListener(this);
    }
    if (member instanceof TypeListener) {
      registerTypeListener((TypeListener) member);
    }
    if (this.access == AccessValues.WRITE) {
      this.addNextDFG(this.member);
    } else if (this.access == AccessValues.READ) {
      this.addPrevDFG(this.member);
    } else {
      this.addNextDFG(this.member);
      this.addPrevDFG(this.member);
    }
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
        .append("base", base)
        .append("member", member)
        .toString();
  }

  public void setAccess(AccessValues access) {
    if (this.access == AccessValues.WRITE) {
      this.removeNextDFG(this.member);
    } else if (this.access == AccessValues.READ) {
      this.removePrevDFG(this.member);
    } else {
      this.removeNextDFG(this.member);
      this.removePrevDFG(this.member);
    }

    this.access = access;

    if (this.access == AccessValues.WRITE) {
      this.addNextDFG(this.member);
    } else if (this.access == AccessValues.READ) {
      this.addPrevDFG(this.member);
    } else {
      this.addNextDFG(this.member);
      this.addPrevDFG(this.member);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MemberExpression)) {
      return false;
    }
    MemberExpression that = (MemberExpression) o;
    return super.equals(that)
        && Objects.equals(member, that.member)
        && Objects.equals(base, that.base);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
