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
package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.Collection;
import java.util.Set;

public interface HasType {
  Type getType();

  /**
   * @return The returned Type is always the same as getType() with the exception of ReferenceType
   *     since there is no case in which we want to propagate a reference when using typeChanged()
   */
  Type getPropagationType();

  default void setType(Type type) {
    setType(type, null);
  }

  default void setUnknownType() {
    setUnknownType(null);
  }

  void setUnknownType(HasType root);

  /**
   * Sideeffect free type modification WARNING: This should only be used by the TypeSystem Pass
   *
   * @param type new type
   */
  void updateType(Type type);

  void updatePossibleSubtypes(Set<Type> types);

  /**
   * Set the node's type. This may start a chain of type listener notifications
   *
   * @param type new type
   * @param root The node that initiated the type change chain. When a node receives a type setting
   *     command where root == this, we know that we have a type listener circle and can abort. If
   *     root == null, the type change is seen as an externally triggered event and subsequent type
   *     listeners receive the current node as their root.
   */
  void setType(Type type, HasType root);

  Set<Type> getPossibleSubTypes();

  default void setPossibleSubTypes(Set<Type> possibleSubTypes) {
    setPossibleSubTypes(possibleSubTypes, null);
  }

  /**
   * Set the node's possible subtypes. Listener circle detection works the same way as with {@link
   * #setType(Type, HasType)}
   *
   * @param possibleSubTypes
   * @param root
   */
  void setPossibleSubTypes(Set<Type> possibleSubTypes, HasType root);

  void registerTypeListener(TypeListener listener);

  void unregisterTypeListener(TypeListener listener);

  Set<TypeListener> getTypeListeners();

  void refreshType();

  /**
   * Used to set the type and clear the possible subtypes list for when a type is more precise than
   * the current.
   *
   * @param type the more precise type
   */
  void resetTypes(Type type);

  interface TypeListener {

    void typeChanged(HasType src, HasType root, Type oldType);

    void possibleSubTypesChanged(HasType src, HasType root, Set<Type> oldSubTypes);

    default void typeChangedToUnknown(HasType src, HasType root) {
      if (this instanceof HasType) {
        ((HasType) this).setUnknownType(root);
      }
    }
  }

  /**
   * The Typeresolver needs to be aware of all outgoing edges to types in order to merge equal types
   * to the same node. For the primary type edge, this is achieved through the hasType interface. If
   * a node has additional type edges (e.g. default type in {@link
   * de.fraunhofer.aisec.cpg.graph.declarations.TypeParamDeclaration}) the node must implement the
   * updateType method, so that the current type is always replaced with the merged one
   */
  interface SecondaryTypeEdge {
    void updateType(Collection<Type> typeState);
  }
}
