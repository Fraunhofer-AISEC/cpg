package de.fraunhofer.aisec.cpg.passes.scopes;

import de.fraunhofer.aisec.cpg.graph.Node;

public class TemplateScope extends NameScope {
  public TemplateScope(Node node, String currentPrefix, String delimiter) {
    super(node, currentPrefix, delimiter);
  }
}
