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
package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

public class Annotation extends Node {

  private List<AnnotationMember> members;

  public List<AnnotationMember> getMembers() {
    return members;
  }

  public void setMembers(List<AnnotationMember> members) {
    this.members = members;
  }

  @Nullable
  public Expression getValueForName(String name) {
    return members.stream()
        .filter(member -> member.getName().endsWith(name))
        .map(AnnotationMember::getValue)
        .findAny()
        .orElse(null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Annotation)) {
      return false;
    }

    Annotation that = (Annotation) o;
    return super.equals(that) && Objects.equals(members, that.members);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
