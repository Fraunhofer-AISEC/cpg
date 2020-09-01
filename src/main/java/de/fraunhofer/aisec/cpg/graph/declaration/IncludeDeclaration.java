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

package de.fraunhofer.aisec.cpg.graph.declaration;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class IncludeDeclaration extends Declaration {

  @SubGraph("AST")
  private List<IncludeDeclaration> includes = new ArrayList<>();

  @SubGraph("AST")
  private List<ProblemDeclaration> problems = new ArrayList<>();

  private String filename;

  public List<IncludeDeclaration> getIncludes() {
    return includes;
  }

  public List<ProblemDeclaration> getProblems() {
    return problems;
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
        && Objects.equals(problems, that.problems)
        && Objects.equals(filename, that.filename);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
