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

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents a single declaration or definition, i.e. of a variable ({@link VariableDeclaration})
 * or function ({@link FunctionDeclaration}).
 *
 * <p>Note: We do NOT (currently) distinguish between the definition and the declaration of a
 * function. This means, that if a function is first declared and later defined with a function
 * body, we will currently have two {@link FunctionDeclaration} nodes. This is very similar to the
 * behaviour of clang, however clang does establish a connection between those nodes, we currently
 * do not.
 */
// TODO: expressionRefersToDeclaration definition and declaration nodes and introduce a field if its
// declaration only
public class Declaration extends Node {

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE).toString();
  }
}
