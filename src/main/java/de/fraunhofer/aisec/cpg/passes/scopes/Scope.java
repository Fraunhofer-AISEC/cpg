/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes.scopes;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.statements.LabelStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represent semantic scopes in the language and only saves information, such as relevant
 * statements. Pre and Postprocessing is done by the passes. Only the passes themselves know the
 * semantics of used edges, but different passes can use the same scope stack concept.
 */
public abstract class Scope {

  protected Node astNode;

  // FQN Name currently valid
  protected String scopedName;

  /* Scopes are nested and therefore have a parent child relationship, this two members will help
  navigate through the scopes,e.g. when looking up variables */
  protected Scope parent = null;
  protected List<Scope> children = new ArrayList<>();

  protected Map<String, LabelStatement> labelStatements = new HashMap<>();

  public String getScopedName() {
    return scopedName;
  }

  public void setScopedName(String scopedName) {
    this.scopedName = scopedName;
  }

  public Node getAstNode() {
    return astNode;
  }

  public void setAstNode(Node astNode) {
    this.astNode = astNode;
  }

  public Map<String, LabelStatement> getLabelStatements() {
    return labelStatements;
  }

  public void setLabelStatements(Map<String, LabelStatement> labelStatements) {
    this.labelStatements = labelStatements;
  }

  public void addLabelStatement(LabelStatement labelStatement) {
    labelStatements.put(labelStatement.getLabel(), labelStatement);
  }

  public Scope getParent() {
    return parent;
  }

  public void setParent(Scope parent) {
    this.parent = parent;
  }

  public List<Scope> getChildren() {
    return children;
  }

  public void setChildren(List<Scope> children) {
    this.children = children;
  }
}
