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

package de.fraunhofer.aisec.cpg.graph.declarations;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.Relationship;

public class IncludeDeclaration extends Declaration {

  @Relationship(value = "includes", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge> includes = new ArrayList<>();

  @Relationship(value = "problems", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge> problems = new ArrayList<>();

  private String filename;

  public List<IncludeDeclaration> getIncludes() {
    List<IncludeDeclaration> target = new ArrayList<>();
    for (PropertyEdge propertyEdge : this.includes) {
      target.add((IncludeDeclaration) propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(target);
  }

  public List<PropertyEdge> getIncludesPropertyEdge() {
    return this.includes;
  }

  public void addInclude(IncludeDeclaration includeDeclaration) {
    PropertyEdge propertyEdge = new PropertyEdge(this, includeDeclaration);
    propertyEdge.addProperty(Properties.INDEX, this.includes.size());
    this.includes.add(propertyEdge);
  }

  public List<ProblemDeclaration> getProblems() {
    List<ProblemDeclaration> target = new ArrayList<>();
    for (PropertyEdge propertyEdge : this.problems) {
      target.add((ProblemDeclaration) propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(target);
  }

  public List<PropertyEdge> getProblemsPropertyEdge() {
    return this.problems;
  }

  public void addProblems(Collection<ProblemDeclaration> c) {
    for (ProblemDeclaration problemDeclaration : c) {
      addProblem(problemDeclaration);
    }
  }

  public void addProblem(ProblemDeclaration problemDeclaration) {
    PropertyEdge propertyEdge = new PropertyEdge(this, problemDeclaration);
    propertyEdge.addProperty(Properties.INDEX, this.problems.size());
    this.problems.add(propertyEdge);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("name", name)
        .append("filename", filename)
        .append("includes", includes)
        .append("problems", problems)
        .toString();
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IncludeDeclaration)) {
      return false;
    }
    IncludeDeclaration that = (IncludeDeclaration) o;
    return super.equals(that)
        && Objects.equals(includes, that.includes)
        && Objects.equals(this.getIncludes(), that.getIncludes())
        && Objects.equals(problems, that.problems)
        && Objects.equals(this.getProblems(), that.getProblems())
        && Objects.equals(filename, that.filename);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
