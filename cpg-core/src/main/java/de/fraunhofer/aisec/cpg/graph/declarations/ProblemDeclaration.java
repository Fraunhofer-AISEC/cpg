/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations;

import de.fraunhofer.aisec.cpg.graph.Node;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ProblemDeclaration extends ValueDeclaration {
  private String filename;
  private String problem;
  private String problemLocation;

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("filename", filename)
        .append("problem", problem)
        .append("problemLocation", problemLocation)
        .toString();
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getProblem() {
    return problem;
  }

  public void setProblem(String problem) {
    this.problem = problem;
  }

  public String getProblemLocation() {
    return problemLocation;
  }

  public void setProblemLocation(String problemLocation) {
    this.problemLocation = problemLocation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProblemDeclaration)) {
      return false;
    }
    ProblemDeclaration that = (ProblemDeclaration) o;
    return super.equals(that)
        && Objects.equals(filename, that.filename)
        && Objects.equals(problem, that.problem)
        && Objects.equals(problemLocation, that.problemLocation);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
