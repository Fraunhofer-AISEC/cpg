package de.fraunhofer.aisec.cpg.passes.scopes;

import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

public class StructureDeclarationScope extends ValueDeclarationScope {

  public List<Declaration> getStructureDeclarations() {
    return structureDeclarations;
  }

  public void setStructureDeclarations(@NonNull List<Declaration> structureDeclarations) {
    this.structureDeclarations = structureDeclarations;
  }

  @NonNull private List<Declaration> structureDeclarations = new ArrayList<>();

  public StructureDeclarationScope(Node node) {
    super(node);
  }

  private void addStructureDeclaration(@NonNull Declaration declaration) {
    structureDeclarations.add(declaration);
  }

  public void addDeclaration(@NonNull Declaration declaration) {
    if (declaration instanceof ValueDeclaration) {
      addValueDeclaration((ValueDeclaration) declaration);
    } else {
      addStructureDeclaration(declaration);
      if (astNode instanceof FunctionDeclaration) {
        FunctionDeclaration functionD = (FunctionDeclaration) astNode;
        if (declaration instanceof RecordDeclaration) {
          addByNameAndReplaceIfNotContained(
              functionD.getRecords(), (RecordDeclaration) declaration);
        }

      } else if (astNode instanceof NamespaceDeclaration) {
        NamespaceDeclaration nameD = (NamespaceDeclaration) astNode;

        if (declaration instanceof RecordDeclaration) {
          addByNameAndReplaceIfNotContained(nameD.getRecords(), (RecordDeclaration) declaration);
        } else if (declaration instanceof NamespaceDeclaration) {
          addByNameAndReplaceIfNotContained(
              nameD.getNamespaces(), (NamespaceDeclaration) declaration);
        }

      } else if (astNode instanceof RecordDeclaration) {
        RecordDeclaration recordD = (RecordDeclaration) astNode;
        if (declaration instanceof RecordDeclaration) {
          addByNameAndReplaceIfNotContained(recordD.getRecords(), (RecordDeclaration) declaration);
        } else if (declaration instanceof FieldDeclaration) {
          addByNameAndReplaceIfNotContained(recordD.getFields(), (FieldDeclaration) declaration);
        }

      } else if (this instanceof GlobalScope) {
        // Doe not have to be added in a declaration scope but could be added to the
        // translationUnitDeclaration
      }
    }
  }
}
