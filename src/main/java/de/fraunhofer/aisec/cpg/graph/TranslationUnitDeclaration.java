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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

/** The top most declaration, representing a translation unit, for example a file. */
public class TranslationUnitDeclaration extends Declaration {

  /** A list of declarations within this unit. */
  @SubGraph("AST")
  private List<Declaration> declarations = new ArrayList<>();

  /** A list of includes within this unit. */
  @SubGraph("AST")
  private List<Declaration> includes = new ArrayList<>();

  /** A list of namespaces within this unit. */
  @SubGraph("AST")
  private List<Declaration> namespaces = new ArrayList<>();

  public <T> T getDeclarationAs(int i, Class<T> clazz) {
    return clazz.cast(this.declarations.get(i));
  }

  public List<Declaration> getDeclarations() {
    return declarations;
  }

  public void setDeclarations(List<Declaration> declarations) {
    this.declarations = declarations;
  }

  public List<Declaration> getIncludes() {
    return includes;
  }

  public void setIncludes(List<Declaration> includes) {
    this.includes = includes;
  }

  public List<Declaration> getNamespaces() {
    return namespaces;
  }

  public void add(Declaration decl) {
    if (decl instanceof IncludeDeclaration) {
      includes.add(decl);
    } else {
      declarations.add(decl);
    }
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .append("declarations", declarations)
        .append("includes", includes)
        .append("namespaces", namespaces)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TranslationUnitDeclaration)) {
      return false;
    }
    TranslationUnitDeclaration that = (TranslationUnitDeclaration) o;
    return super.equals(that)
        && Objects.equals(declarations, that.declarations)
        && Objects.equals(includes, that.includes)
        && Objects.equals(namespaces, that.namespaces);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
