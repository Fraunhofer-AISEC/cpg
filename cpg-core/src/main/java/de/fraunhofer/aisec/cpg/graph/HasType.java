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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;

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
   * @param root The nodes which we have seen in the type change chain. When a node receives a type
   *     setting command where root.contains(this), we know that we have a type listener circle and
   *     can abort. If root is an empty list, the type change is seen as an externally triggered
   *     event and subsequent type listeners receive the current node as their root.
   */
  void setType(Type type, Collection<HasType> root);

  Set<Type> getPossibleSubTypes();

  default void setPossibleSubTypes(Set<Type> possibleSubTypes) {
    setPossibleSubTypes(possibleSubTypes, new ArrayList<>());
  }

  /**
   * Set the node's possible subtypes. Listener circle detection works the same way as with {@link
   * #setType(Type, Collection<HasType>)}
   *
   * @param possibleSubTypes
   * @param root A list of already seen nodes which is used for detecting loops.
   */
  void setPossibleSubTypes(Set<Type> possibleSubTypes, @NonNull Collection<HasType> root);

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

    void typeChanged(HasType src, Collection<HasType> root, Type oldType);

    void possibleSubTypesChanged(HasType src, Collection<HasType> root, Set<Type> oldSubTypes);
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
