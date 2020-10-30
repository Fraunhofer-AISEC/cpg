package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.types.*;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.util.*;

public class TypeResolver extends Pass {
  private Set<Type> firstOrderTypes = new HashSet<>();
  private Map<Type, List<Type>> typeState = new HashMap<>();

  /**
   * Reduce the SecondOrderTypes to store only the unique SecondOrderTypes
   *
   * @param type SecondOrderType that is to be eliminated if an equal is already in typeState or is
   *     added if not
   */
  private void processSecondOrderTypes(Type type) {
    Type root = type.getRoot();

    if (typeState.get(root).contains(type)) {
      return;
    }

    typeState.get(root).add(type);
    Type element = null;

    if (type instanceof SecondOrderType) {
      element = ((SecondOrderType) type).getElementType();
    }

    assert element != null;
    if (!(element instanceof SecondOrderType)) {
      return;
    }
    Type finalElement = element;
    Type newElement =
        typeState.get(root).stream().filter(t -> t.equals(finalElement)).findAny().orElse(null);

    if (newElement != null) {
      ((SecondOrderType) type).setElementType(newElement);
    } else {
      processSecondOrderTypes(element);
    }
  }

  /**
   * Ensures that two different Types that are created at different Points are still the same object
   * in order to only store one node into the database
   *
   * @param type newly created Type
   * @return If the same type was already stored in the typeState Map the stored one is returned. In
   *     the other case the parameter type is stored into the map and the parameter type is returned
   */
  public Type obtainType(Type type) {
    if (type == null) {
      return null;
    }
    Type root = type.getRoot();
    if (root.equals(type) && typeState.containsKey(type)) {
      for (Type t : typeState.keySet()) {
        if (t.equals(type)) {
          return t;
        }
      }
    } else {
      addType(type);
      return type;
    }

    if (typeState.containsKey(root)) {
      List<Type> references = typeState.get(root);
      for (Type r : references) {
        if (r.equals(type)) {
          return r;
        }
      }
      addType(type);
      return type;
    }

    addType(type);
    return type;
  }

  /**
   * Responsible for storing new types into typeState
   *
   * @param type new type
   */
  private void addType(Type type) {
    Type root = type.getRoot();
    if (root.equals(type) && !typeState.containsKey(type)) {
      // This is a rootType and is included in the map as key with empty references
      typeState.put(type, new ArrayList<>());
      return;
    }

    // ReferencesTypes
    if (typeState.containsKey(root)) {
      if (!typeState.get(root).contains(type)) {
        typeState.get(root).add(type);
        addType(((SecondOrderType) type).getElementType());
      }

    } else {
      addType(type.getRoot());
      addType(type);
    }
  }

  private void removeDuplicateTypes() {
    TypeManager typeManager = TypeManager.getInstance();
    // Remove duplicate firstOrderTypes
    firstOrderTypes.addAll(typeManager.getFirstOrderTypes());

    // Propagate new firstOrderTypes into secondOrderTypes
    Set<Type> secondOrderTypes = typeManager.getSecondOrderTypes();
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

    // Build Map from firstOrderTypes to list of secondOderTypes
    for (Type t : firstOrderTypes) {
      typeState.put(t, new ArrayList<>());
    }

    // Remove duplicate secondOrderTypes
    for (Type t : secondOrderTypes) {
      processSecondOrderTypes(t);
    }

    // Remove duplicates from fields
    for (Type t : secondOrderTypes) {
      removeDuplicatesInFields(t);
    }
  }

  /**
   * Visits all FirstOrderTypes and replace all the fields like returnVal or parameters for
   * FunctionPointertype or Generics for ObjectType
   *
   * @param t FirstOrderType
   */
  private void removeDuplicatesInFields(Type t) {
    // Remove duplicates from fields
    if (t instanceof FunctionPointerType) {
      ((FunctionPointerType) t)
          .setReturnType(obtainType(((FunctionPointerType) t).getReturnType()));
      List<Type> newParameters = new ArrayList<>();
      for (Type t2 : ((FunctionPointerType) t).getParameters()) {
        newParameters.add(obtainType(t2));
      }
      ((FunctionPointerType) t).setParameters(newParameters);
    } else if (t instanceof ObjectType) {
      List<Type> newGenerics = new ArrayList<>();
      for (Type generic : ((ObjectType) t).getGenerics()) {
        newGenerics.add(obtainType(generic));
      }
      ((ObjectType) t).setGenerics(newGenerics);
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
    TypeParser.reset();
    TypeManager.reset();
  }
}
