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

/**
 * INDEX:(int) Indicates the position in a list of edges
 *
 * <p>BRANCH:(boolean) If we have multiple EOG edges the branch property indicates which EOG edge
 * leads to true branch (expression evaluated to true) or the false branch (e.g. with an if/else
 * condition)
 *
 * <p>DEFAULT:(boolean) Indicates which arguments edge of a CallExpression leads to a default
 * argument
 *
 * <p>NAME:(string) An optional name for the property edge
 *
 * <p>[UNREACHABLE]:(boolean) True if the edge flows into unreachable code i.e. a branch condition
 * which is always false.
 */
public enum Properties {
  INDEX,
  BRANCH,
  NAME,
  INSTANTIATION,
  UNREACHABLE
}
