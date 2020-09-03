package de.fraunhofer.aisec.cpg.passes.scopes;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration;
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
          addIfNotContained(functionD.getRecords(), (RecordDeclaration) declaration);
        }

      } else if (astNode instanceof NamespaceDeclaration) {
        NamespaceDeclaration nameD = (NamespaceDeclaration) astNode;

        addIfNotContained(nameD.getDeclarations(), declaration);

      } else if (astNode instanceof RecordDeclaration) {
        RecordDeclaration recordD = (RecordDeclaration) astNode;
        if (declaration instanceof RecordDeclaration) {
          addIfNotContained(recordD.getRecords(), (RecordDeclaration) declaration);
        } else if (declaration instanceof FieldDeclaration) {
          addIfNotContained(recordD.getFields(), (FieldDeclaration) declaration);
        }

      } else if (this instanceof GlobalScope) {
        // Doe not have to be added in a declaration scope but could be added to the
        // translationUnitDeclaration
      }
    }
  }
}
