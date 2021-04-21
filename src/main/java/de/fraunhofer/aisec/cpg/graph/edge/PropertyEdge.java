/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.edge;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.Persistable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.ogm.annotation.*;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RelationshipEntity()
public class PropertyEdge<T extends Node> implements Persistable {

  /** Required field for object graph mapping. It contains the node id. */
  @Id @GeneratedValue private Long id;

  protected static final Logger log = LoggerFactory.getLogger(PropertyEdge.class);

  // Node where the edge is outgoing
  @StartNode private Node start;

  // Node where the edge is ingoing
  @EndNode private T end;

  public PropertyEdge(Node start, T end) {
    this.start = start;
    this.end = end;
    this.properties = new EnumMap<>(Properties.class);
  }

  public PropertyEdge(PropertyEdge<T> propertyEdge) {
    this.start = propertyEdge.start;
    this.end = propertyEdge.end;
    this.properties = new EnumMap<>(Properties.class);
    this.properties.putAll(propertyEdge.properties);
  }

  public PropertyEdge(Node start, T end, Map<Properties, Object> properties) {
    this.start = start;
    this.end = end;
    this.properties = properties;
  }

  /** Map containing all properties of an edge */
  @Convert(PropertyEdgeConverter.class)
  private Map<Properties, Object> properties;

  public static <S extends PropertyEdge<?>> List<S> findPropertyEdgesByPredicate(
      Collection<S> edges, Predicate<S> predicate) {
    return edges.stream().filter(predicate).collect(Collectors.toList());
  }

  public Object getProperty(Properties property) {
    return properties.getOrDefault(property, null);
  }

  /**
   * Adds a property to a {@link PropertyEdge} If the object is not a built-in type you must provide
   * a serializer and deserializer in the {@link PropertyEdgeConverter}
   *
   * @param property String containing the name of the property
   * @param value Object containing the value of the property
   */
  public void addProperty(Properties property, Object value) {
    properties.put(property, value);
  }

  public void addProperties(Map<Properties, Object> propertyMap) {
    this.properties.putAll(propertyMap);
  }

  public T getEnd() {
    return end;
  }

  public Node getStart() {
    return start;
  }

  public void setEnd(T end) {
    this.end = end;
  }

  public void setStart(Node start) {
    this.start = start;
  }

  /**
   * Add/Update index element of list of PropertyEdges
   *
   * @param propertyEdges propertyEdge list
   * @return new PropertyEdge list with updated index property
   */
  public static <T extends Node> List<PropertyEdge<T>> applyIndexProperty(
      List<PropertyEdge<T>> propertyEdges) {
    int counter = 0;
    for (PropertyEdge<T> propertyEdge : propertyEdges) {
      propertyEdge.addProperty(Properties.INDEX, counter);
      counter++;
    }
    return propertyEdges;
  }

  /**
   * Transforms a List of Nodes into targets of PropertyEdges. Include Index Property as Lists are
   * indexed
   *
   * @param nodes List of nodes that should be transformed into PropertyEdges
   * @param commonRelationshipNode node where all the Edges should start
   * @return List of PropertyEdges with the targets of the nodes and index property.
   */
  public static <T extends Node> List<PropertyEdge<T>> transformIntoOutgoingPropertyEdgeList(
      List<T> nodes, Node commonRelationshipNode) {
    List<PropertyEdge<T>> propertyEdges = new ArrayList<>();
    for (T n : nodes) {
      propertyEdges.add(new PropertyEdge<>(commonRelationshipNode, n));
    }
    return propertyEdges;
  }

  /**
   * Transforms a List of Nodes into sources of PropertyEdges. Include Index Property as Lists are
   * indexed
   *
   * @param nodes List of nodes that should be transformed into PropertyEdges
   * @param commonRelationshipNode node where all the Edges should end.
   * @return List of PropertyEdges with the nodes as sources and index property.
   */
  public static <T extends Node> List<PropertyEdge<T>> transformIntoIncomingPropertyEdgeList(
      List<? extends Node> nodes, T commonRelationshipNode) {
    List<PropertyEdge<T>> propertyEdges = new ArrayList<>();
    for (Node n : nodes) {
      propertyEdges.add(new PropertyEdge<>(n, commonRelationshipNode));
    }
    return propertyEdges;
  }

  /**
   * Unwraps this outgoing property edge into a list of its target nodes.
   *
   * @param collection the collection of edges
   * @param <T> the type of the edges
   * @return the list of target nodes
   */
  public static <T extends Node> List<T> unwrap(@NonNull List<PropertyEdge<T>> collection) {
    return unwrap(collection, true);
  }

  /**
   * Unwraps this property edge into a list of its target nodes.
   *
   * @param collection the collection of edges
   * @param outgoing whether it is outgoing or not
   * @param <T> the type of the edges
   * @return the list of target nodes
   */
  public static <T extends Node> List<T> unwrap(
      @NonNull List<PropertyEdge<T>> collection, boolean outgoing) {
    return collection.stream()
        .map(edge -> outgoing ? edge.getEnd() : (T) edge.getStart())
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * @param collection is a collection that presumably holds property edges
   * @param outgoing direction of the edges
   * @return collection of nodes containing the targets of the edges
   */
  private static Object unwrapPropertyEdgeCollection(Collection<?> collection, boolean outgoing) {
    Object element = null;
    Optional<?> value = collection.stream().findAny();
    if (value.isPresent()) {
      element = value.get();
    }

    if (element instanceof PropertyEdge) {
      try {
        var outputCollection =
            (Collection<Node>) collection.getClass().getDeclaredConstructor().newInstance();
        for (var obj : collection) {
          if (obj instanceof PropertyEdge) {
            var propertyEdge = (PropertyEdge<?>) obj;

            if (outgoing) {
              outputCollection.add(propertyEdge.getEnd());
            } else {
              outputCollection.add(propertyEdge.getStart());
            }
          }
        }
        return outputCollection;
      } catch (InstantiationException
          | IllegalAccessException
          | InvocationTargetException
          | NoSuchMethodException e) {
        log.warn("PropertyEdges could not be unwrapped");
      }
    }
    return collection;
  }

  /**
   * @param obj PropertyEdge or collection of property edges that must be unwrapped
   * @param outgoing direction of the edge
   * @return node or collection representing target of edge
   */
  public static Object unwrapPropertyEdge(Object obj, boolean outgoing) {
    if (obj instanceof PropertyEdge) {
      if (outgoing) {
        return ((PropertyEdge<?>) obj).getEnd();
      } else {
        return ((PropertyEdge<?>) obj).getStart();
      }
    } else if (obj instanceof Collection && !((Collection<?>) obj).isEmpty()) {
      return unwrapPropertyEdgeCollection((Collection<?>) obj, outgoing);
    }
    return obj;
  }

  /**
   * Checks if an Object is a PropertyEdge or a collection of PropertyEdges
   *
   * @param f Field containing the object
   * @param obj object that is checked if it is a PropertyEdge
   * @return true if obj is/contains a PropertyEdge
   */
  public static boolean checkForPropertyEdge(Field f, Object obj) {
    if (obj instanceof PropertyEdge) {
      return true;
    } else if (obj instanceof Collection<?>) {
      List<Type> collectionTypes =
          List.of(((ParameterizedType) f.getGenericType()).getActualTypeArguments());
      for (Type t : collectionTypes) {
        if (t instanceof ParameterizedType) {
          return ((ParameterizedType) t).getRawType().equals(PropertyEdge.class);
        } else if (PropertyEdge.class.equals(t)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @param propertyEdges List of PropertyEdges
   * @return List of nodes corresponding to the targets of the edges
   */
  public static <T extends Node> List<T> getTarget(List<PropertyEdge<T>> propertyEdges) {
    List<T> targets = new ArrayList<>();
    for (var propertyEdge : propertyEdges) {
      targets.add(propertyEdge.getEnd());
    }
    return targets;
  }

  /**
   * @param propertyEdges List of PropertyEdges
   * @return List of nodes corresponding to the targets of the edges
   */
  public static <T extends Node> List<Node> getSource(List<PropertyEdge<T>> propertyEdges) {
    List<Node> targets = new ArrayList<>();
    for (var propertyEdge : propertyEdges) {
      targets.add(propertyEdge.getStart());
    }
    return targets;
  }

  public static <T extends Node> List<PropertyEdge<T>> removeElementFromList(
      List<PropertyEdge<T>> propertyEdges, T element, boolean end) {
    List<PropertyEdge<T>> newPropertyEdges = new ArrayList<>();
    for (var propertyEdge : propertyEdges) {
      if (end && !propertyEdge.getEnd().equals(element)) {
        newPropertyEdges.add(propertyEdge);
      }
      if (!end && !propertyEdge.getStart().equals(element)) {
        newPropertyEdges.add(propertyEdge);
      }
    }
    return applyIndexProperty(newPropertyEdges);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PropertyEdge)) return false;
    var propertyEdge = (PropertyEdge<?>) obj;
    return Objects.equals(this.properties, propertyEdge.properties)
        && this.start.equals(propertyEdge.getStart())
        && this.end.equals(propertyEdge.getEnd());
  }

  public boolean propertyEquals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PropertyEdge)) return false;
    var propertyEdge = (PropertyEdge<?>) obj;
    return Objects.equals(this.properties, propertyEdge.properties);
  }

  public static <E extends Node> boolean propertyEqualsList(
      List<PropertyEdge<E>> propertyEdges, List<PropertyEdge<E>> propertyEdges2) {
    if (propertyEdges.size() == propertyEdges2.size()) {
      for (int i = 0; i < propertyEdges.size(); i++) {
        if (!propertyEdges.get(i).propertyEquals(propertyEdges2.get(i))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(end, properties);
  }
}
