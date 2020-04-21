package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.type.ObjectType;
import de.fraunhofer.aisec.cpg.graph.type.PointerType;
import de.fraunhofer.aisec.cpg.graph.type.ReferenceType;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.util.*;

public class TypeClassResolver extends Pass {
  private Set<Type> firstOrderTypes = new HashSet<>();
  private Map<Type, List<Type>> typeState = new HashMap<>();

  private void processSecondOrderTypes(Type type) {
    Type root = type.getRoot();
    if (!typeState.get(root).contains(type)) {
      typeState.get(root).add(type);
      Type element = null;
      if (type instanceof PointerType) {
        element = ((PointerType) type).getElementType();
      } else if (type instanceof ReferenceType) {
        element = ((ReferenceType) type).getReference();
      }
      assert element != null;
      if (!element.isFirstOrderType()) {
        if (typeState.get(root).contains(element)) {
          for (Type t : typeState.get(root)) {
            if (t.equals(element)) {
              if (type instanceof PointerType) {
                ((PointerType) type).setElementType(t);
              } else if (type instanceof ReferenceType) {
                ((ReferenceType) type).setReference(t);
              }
            }
          }
        } else {
          processSecondOrderTypes(element);
        }
      }
    }
  }

  private void removeDuplicateTypes() {
    TypeManager typeManager = TypeManager.getInstance();
    // 1. Step: Remove duplicate firstOrderTypes
    firstOrderTypes.addAll(typeManager.getFirstOrderTypes());

    // 2. Step: Propagate new firstOrderTypes into secondOrderTypes
    List<Type> secondOrderTypes = typeManager.getSecondOrderTypes();
    for (Type t : secondOrderTypes) {
      Type root = t.getRoot();
      Type newRoot = root;
      for (Type firstOrderRoot : firstOrderTypes) {
        if (firstOrderRoot.equals(root)) {
          newRoot = firstOrderRoot;
          break;
        }
      }
      t.setRoot(newRoot);
    }

    // 3. Step: Build Map from firstOrderTypes to list of secondOderTypes
    for (Type t : firstOrderTypes) {
      typeState.put(t, new ArrayList<>());
    }

    // 4. Step: Remove duplicate secondOrderTypes
    for (Type t : secondOrderTypes) {
      processSecondOrderTypes(t);
    }
  }

  /**
   * Pass on the TypeSystem: Sets RecordDeclaration Relationship from ObjectType to
   * RecordDeclaration
   *
   * @param translationResult
   */
  @Override
  public void accept(TranslationResult translationResult) {

    removeDuplicateTypes();

    SubgraphWalker.IterativeGraphWalker walker = new SubgraphWalker.IterativeGraphWalker();
    walker.registerOnNodeVisit(this::ensureUniqueType);
    walker.registerOnNodeVisit(this::handle);
    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      walker.iterate(tu);
    }
  }

  public Set<Type> ensureUniqueSubTypes(Set<Type> subTypes) {
    Set<Type> uniqueTypes = new HashSet<>();
    for (Type subType : subTypes) {
      Collection<Type> trackedTypes;
      if (subType.isFirstOrderType()) {
        trackedTypes = typeState.keySet();
      } else {
        Type root = subType.getRoot();
        trackedTypes = typeState.get(root);
      }

      for (Type t : trackedTypes) {
        if (t.equals(subType)) {
          uniqueTypes.add(t);
          break;
        }
      }
    }
    return uniqueTypes;
  }

  public void ensureUniqueType(Node node) {
    if (node instanceof HasType) {
      Type oldType = ((HasType) node).getType();
      Collection<Type> types;
      if (oldType.isFirstOrderType()) {
        types = typeState.keySet();
      } else {
        Type root = oldType.getRoot();
        types = typeState.get(root);
      }

      for (Type t : types) {
        if (t.equals(oldType)) {
          ((HasType) node).updateType(t);
          break;
        }
      }

      ((HasType) node)
          .updatePossibleSubtypes(ensureUniqueSubTypes(((HasType) node).getPossibleSubTypes()));
    }
  }

  /**
   * Creates the recordDeclaration relationship between ObjectTypes and RecordDeclaration (from the
   * Type to the Class)
   *
   * @param node
   */
  public void handle(Node node) {
    if (node instanceof RecordDeclaration) {
      String recordDeclarationName = node.getName();
      for (Type t : typeState.keySet()) {
        if (t.getTypeName().equals(recordDeclarationName) && t instanceof ObjectType) {
          ((ObjectType) t).setRecordDeclaration((RecordDeclaration) node);
        }
      }
    }
  }

  @Override
  public void cleanup() {
    this.firstOrderTypes.clear();
    this.typeState.clear();
  }
}
