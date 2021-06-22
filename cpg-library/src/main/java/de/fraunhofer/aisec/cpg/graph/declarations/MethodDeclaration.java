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

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.AstChild;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A method declaration is a {@link FunctionDeclaration} tied to a specific {@link
 * RecordDeclaration}.
 */
public class MethodDeclaration extends FunctionDeclaration {

  private boolean isStatic;

  /**
   * The {@link RecordDeclaration} this method is tied to. This can be empty if we do not know about
   * the type.
   */
  @Nullable private RecordDeclaration recordDeclaration;

  /**
   * The receiver variable of this method. In most cases, this variable is called 'this', but in
   * some languages, it is 'self' (e.g. in Rust or Python) or can be freely named (e.g. in Golang).
   *
   * <p>It can be empty, i.e., for pure function definitions as part as an interface.
   */
  @SubGraph("AST")
  @Nullable
  private AstChild<VariableDeclaration> receiver;

  public boolean isStatic() {
    return isStatic;
  }

  public void setStatic(boolean isStatic) {
    this.isStatic = isStatic;
  }

  @Nullable
  public RecordDeclaration getRecordDeclaration() {
    return recordDeclaration;
  }

  public void setRecordDeclaration(@Nullable RecordDeclaration recordDeclaration) {
    this.recordDeclaration = recordDeclaration;
  }

  @Nullable
  public VariableDeclaration getReceiver() {
    return receiver != null ? receiver.getEnd() : null;
  }

  public void setReceiver(@Nullable VariableDeclaration receiver) {
    if (receiver != null) {
      this.receiver = new AstChild<>(this, receiver);
    }
  }
}
