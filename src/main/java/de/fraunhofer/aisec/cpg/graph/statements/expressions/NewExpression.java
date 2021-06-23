/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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

package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.TypeManager.Language;
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;

/** Represents the creation of a new object through the <code>new</code> keyword. */
public class NewExpression extends Expression implements TypeListener {

  /** The initializer expression. */
  @SubGraph("AST")
  private Expression initializer;

  public Expression getInitializer() {
    return initializer;
  }

  public void setInitializer(Expression initializer) {
    // TODO: The VariableDeclaration::setInitializer does some DFG stuff. Needed here aswell?
    if (this.initializer != null) {
      this.initializer.unregisterTypeListener(this);
    }
    if (this.initializer instanceof TypeListener) {
      this.unregisterTypeListener((TypeListener) this.initializer);
    }

    this.initializer = initializer;

    if (initializer != null) {
      initializer.registerTypeListener(this);
    }
    // if the initializer implements a type listener, inform it about our type changes
    // since the type is tied to the declaration but it is convenient to have the type
    // information in the initializer, i.e. in a ConstructExpression.
    if (initializer instanceof TypeListener) {
      this.registerTypeListener((TypeListener) initializer);
    }
  }

  @Override
  public void typeChanged(HasType src, HasType root, Type oldType) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }

    Type newType;
    if (TypeManager.getInstance().getLanguage() == Language.CXX && src == initializer) {
      newType = src.getPropagationType().reference(PointerOrigin.POINTER);
    } else {
      newType = src.getPropagationType();
    }

    Type previous = this.type;
    setType(newType, root);
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Type.Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, HasType root, Set<Type> oldSubTypes) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }

    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.addAll(
            src.getPossibleSubTypes().stream()
                    .map(
                            t -> {
                              if (TypeManager.getInstance().getLanguage() == Language.CXX
                                      && src == initializer) {
                                return t.reference(PointerOrigin.POINTER);
                              }
                              return t;
                            })
                    .collect(Collectors.toSet()));
    setPossibleSubTypes(subTypes, root);
  }
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("initializer", initializer)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NewExpression)) {
      return false;
    }
    NewExpression that = (NewExpression) o;
    return super.equals(that) && Objects.equals(initializer, that.initializer);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
