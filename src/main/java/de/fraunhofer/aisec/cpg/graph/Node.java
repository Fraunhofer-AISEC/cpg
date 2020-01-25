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

import de.fraunhofer.aisec.cpg.helpers.RegionConverter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The base class for all graph objects that are going to be persisted in the database. */
public class Node {

  public static final ToStringStyle TO_STRING_STYLE = ToStringStyle.SHORT_PREFIX_STYLE;
  protected static final Logger log = LoggerFactory.getLogger(Node.class);

  public static final String EMPTY_NAME = "";

  /** A human readable name. */
  @NonNull protected String name = EMPTY_NAME; // initialize it with an empty string

  /**
   * Original code snippet of this node. Most nodes will have a corresponding "code", but in cases
   * where nodes are created artificially, it may be null.
   */
  @Nullable protected String code;

  /** Optional comment of this node. */
  @Nullable protected String comment;

  /** Location of the finding in source code. */
  @Convert(RegionConverter.class)
  protected Region region = new Region();

  /** Name of the containing file */
  protected String file;

  /** Incoming control flow edges. */
  @Relationship(value = "EOG", direction = "INCOMING")
  protected List<Node> prevEOG = new ArrayList<>();

  /** outgoing control flow edges. */
  @Relationship(value = "EOG", direction = "OUTGOING")
  @NonNull
  protected List<Node> nextEOG = new ArrayList<>();

  /** outgoing control flow edges. */
  @NonNull
  @Relationship(value = "CFG", direction = "OUTGOING")
  protected List<Node> nextCFG = new ArrayList<>();

  @Relationship(value = "DFG", direction = "INCOMING")
  protected Set<Node> prevDFG = new HashSet<>();

  @Relationship(value = "DFG")
  protected Set<Node> nextDFG = new HashSet<>();

  /**
   * If a node is marked as being a dummy, it means that it was created artificially and does not
   * necessarily have a real counterpart in the actual source code
   */
  protected boolean dummy = false;

  /**
   * Specifies, whether this node is implicit, i.e. is not really existing in source code but only
   * exists implicitly. This mostly relates to implicit casts, return statements or implicit this
   * expressions.
   */
  protected boolean implicit = false;

  /** Required field for object graph mapping. It contains the node id. */
  @Id @GeneratedValue private Long id;

  /** Index of the argument if this node is used in a function call or parameter list. */
  private int argumentIndex;

  public Long getId() {
    return id;
  }

  @NonNull
  public String getName() {
    return name;
  }

  public void setName(@NonNull String name) {
    this.name = name;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  @Nullable
  public String getCode() {
    return this.code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Region getRegion() {
    return this.region;
  }

  public void setRegion(Region region) {
    this.region = region;
  }

  public List<Node> getPrevEOG() {
    return this.prevEOG;
  }

  public void setPrevEOG(List<Node> prevEOG) {
    this.prevEOG = prevEOG;
  }

  @NonNull
  public List<Node> getNextEOG() {
    return this.nextEOG;
  }

  public void setNextEOG(@NonNull List<Node> nextEOG) {
    this.nextEOG = nextEOG;
  }

  @NonNull
  public List<Node> getNextCFG() {
    return this.nextCFG;
  }

  public Set<Node> getNextDFG() {
    return nextDFG;
  }

  public void setNextDFG(Set<Node> nextDFG) {
    this.nextDFG = nextDFG;
  }

  public void addNextDFG(Node next) {
    this.nextDFG.add(next);
  }

  public void removeNextDFG(Node next) {
    if (next != null) {
      this.nextDFG.remove(next);
    }
  }

  public Set<Node> getPrevDFG() {
    return prevDFG;
  }

  public void setPrevDFG(Set<Node> prevDFG) {
    this.prevDFG = prevDFG;
  }

  public void addPrevDFG(Node prev) {
    this.prevDFG.add(prev);
  }

  public void removePrevDFG(Node prev) {
    if (prev != null) {
      this.prevDFG.remove(prev);
    }
  }

  public int getArgumentIndex() {
    return this.argumentIndex;
  }

  public void setArgumentIndex(int argumentIndex) {
    this.argumentIndex = argumentIndex;
  }

  public boolean isDummy() {
    return dummy;
  }

  /** @deprecated You should rather use {@link #setImplicit(boolean)}, if it is an implicit expression */
  @Deprecated(forRemoval = true)
  public void setDummy(boolean dummy) {
    this.dummy = dummy;
  }

  public void setImplicit(boolean implicit) {
    this.implicit = implicit;
  }

  public boolean isImplicit() {
    return this.implicit;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .append("id", id)
        .append("name", name)
        .append("region", region)
        .append("argumentIndex", argumentIndex)
        .toString();
  }

  public void setComment(@NonNull String comment) {
    this.comment = comment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Node)) {
      return false;
    }
    Node node = (Node) o;
    if (region == null
        || region.equals(Region.UNKNOWN_REGION)
        || node.region == null
        || node.region.equals(Region.UNKNOWN_REGION)) {
      // we do not know the exact region. Need to rely on Object equalness,
      // as a different LOC can have the same name/code/comment/file
      return false;
    }
    return Objects.equals(name, node.name)
        && Objects.equals(code, node.code)
        && Objects.equals(comment, node.comment)
        && Objects.equals(region, node.region)
        && Objects.equals(file, node.file);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, this.getClass());
  }
}
