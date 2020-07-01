package de.fraunhofer.aisec.cpg.passes;

import static de.fraunhofer.aisec.cpg.helpers.Util.warnWithFileLocation;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.EnumDeclaration;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.MemberExpression;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.StaticReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import de.fraunhofer.aisec.cpg.helpers.NodeComparator;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceResolverPass extends Pass {

  private static final Logger log = LoggerFactory.getLogger(VariableUsageResolver.class);
  private Map<Type, List<Type>> superTypesMap = new HashMap<>();
  private Map<Type, RecordDeclaration> recordMap = new HashMap<>();
  private Map<Type, EnumDeclaration> enumMap = new HashMap<>();
  private TranslationUnitDeclaration currTu;
  private SubgraphWalker.ScopedWalker walker;

  @Override
  public void cleanup() {
    this.superTypesMap.clear();
    if (this.recordMap != null) {
      this.recordMap.clear();
    }
    this.enumMap.clear();
  }

  @Override
  public void accept(TranslationResult result) {
    walker = new SubgraphWalker.ScopedWalker(lang);

    for (TranslationUnitDeclaration tu : result.getTranslationUnits()) {
      currTu = tu;
      walker.clearCallbacks();
      walker.registerHandler((currClass, parent, currNode) -> walker.collectDeclarations(currNode));
      walker.registerHandler(this::findRecordsAndEnums);
      walker.iterate(currTu);
    }

    Map<Type, List<Type>> currSuperTypes =
        recordMap.values().stream()
            .collect(
                Collectors.toMap(r -> new Type(r.getName()), RecordDeclaration::getSuperTypes));
    superTypesMap.putAll(currSuperTypes);

    // Here it is important to resolve the local variables first and then the field usage, such that
    // the
    // local variable resolution does not overwrite the 'refersTo' resolved by the field usage
    // solver in member expressions.
    for (TranslationUnitDeclaration tu : result.getTranslationUnits()) {
      walker.clearCallbacks();
      walker.registerHandler(this::resolveLocalVarUsage);
      walker.iterate(tu);
    }
    for (TranslationUnitDeclaration tu : result.getTranslationUnits()) {
      walker.clearCallbacks();
      walker.registerHandler(this::resolveFieldUsages);
      walker.iterate(tu);
    }
  }

  public static class WalkingResolver implements Consumer<Node> {

    ScopeManager scopeManager;

    public WalkingResolver(ScopeManager scopeManager) {
      this.scopeManager = scopeManager;
    }

    @Override
    public void accept(Node node) {
      // Uses the multitree scope structure to resolve reverences in the own scope
      scopeManager.enterScope(node);
      if (node instanceof DeclaredReferenceExpression) {
        // Resolving single name references
        ValueDeclaration valueDeclaration =
            scopeManager.resolve((DeclaredReferenceExpression) node);

      } else if (node instanceof MemberExpression) {
        // Resolving memberExpression accessing fields
      }
      List<Node> children = new ArrayList<>(SubgraphWalker.getAstChildren(node));
      children.sort(new NodeComparator());
      children.forEach(child -> accept(child));
      scopeManager.leaveScope(node);
    }
  }

  private void findRecordsAndEnums(Node node, RecordDeclaration curClass) {
    if (node instanceof RecordDeclaration) {
      Type type = new Type(node.getName());
      recordMap.putIfAbsent(type, (RecordDeclaration) node);
    } else if (node instanceof EnumDeclaration) {
      Type type = new Type(node.getName());
      enumMap.putIfAbsent(type, (EnumDeclaration) node);
    }
  }

  private Set<Declaration> resolveFunctionPtr(
      Type containingClass, DeclaredReferenceExpression reference) {
    Set<Declaration> targets = new HashSet<>();
    String functionName = reference.getName();
    Matcher matcher =
        Pattern.compile("(?:(?<class>.*)(?:\\.|::))?(?<function>.*)").matcher(reference.getName());
    if (matcher.matches()) {
      String cls = matcher.group("class");
      functionName = matcher.group("function");
      String finalFunctionName = functionName;
      if (cls == null) {
        targets =
            walker.getAllDeclarationsForScope(reference).stream()
                .filter(FunctionDeclaration.class::isInstance)
                .filter(d -> d.getName().equals(finalFunctionName))
                .collect(Collectors.toCollection(HashSet::new));
      } else {
        containingClass = new Type(cls);
        RecordDeclaration record = resolveRecord(containingClass);
        if (record != null) {
          targets =
              record.getMethods().stream()
                  .filter(f -> f.getName().equals(finalFunctionName))
                  .map(ValueDeclaration.class::cast)
                  .collect(Collectors.toCollection(HashSet::new));
        }
      }
    }

    if (targets.isEmpty()) {
      if (containingClass == null) {
        Set<Declaration> unknownMethod = new HashSet<>();
        unknownMethod.add(handleUnknownMethod(functionName, reference.getType()));
        return unknownMethod;
      } else {
        Set<Declaration> unknownClass = new HashSet<>();
        unknownClass.add(
            handleUnknownClassMethod(containingClass, functionName, reference.getType()));
        return unknownClass;
      }
    } else {
      return targets;
    }
  }

  private RecordDeclaration resolveRecord(@Nullable Type recordType) {
    if (recordType == null) {
      return null;
    }
    // Name may be absolute or relative to the current namespace
    RecordDeclaration recordDeclaration = null;

    // Absolut
    if (recordMap.containsKey(recordType)) {
      recordDeclaration = recordMap.get(recordType);
      // This check wants to determine if the Record is not a Dummy inserted because an entity could
      // not be resolved
      if (recordDeclaration.getLocation() != null && !recordDeclaration.isDummy()) {
        return recordDeclaration;
      }
    }
    String namespacePrefix =
        lang.getScopeManager().getCurrentNamePrefix() + lang.getNamespaceDelimiter();
    do {
      if (recordType == null) {
        System.out.println();
      }
      Type absolutType = new Type(namespacePrefix + recordType.getTypeName());
      // type adjustment etc. are not copied as they should already be stripped before name
      // resolution
      // and no type is declared with them in mind
      if (recordMap.containsKey(absolutType)) {
        return recordMap.get(absolutType);
      }
      // Remove the inner name
      if (namespacePrefix.contains(lang.getNamespaceDelimiter())) {
        namespacePrefix =
            namespacePrefix.substring(0, namespacePrefix.lastIndexOf(lang.getNamespaceDelimiter()));
        if (namespacePrefix.contains(lang.getNamespaceDelimiter())) {
          namespacePrefix =
              namespacePrefix.substring(
                  0,
                  namespacePrefix.lastIndexOf(lang.getNamespaceDelimiter())
                      + lang.getNamespaceDelimiter().length());
        }
      }
    } while (namespacePrefix.contains(lang.getNamespaceDelimiter()));
    return recordDeclaration;
  }

  private void resolveLocalVarUsage(RecordDeclaration currentRecord, Node parent, Node current) {

    currentRecord =
        lang.getScopeManager().isInRecord() ? lang.getScopeManager().getCurrentRecord() : null;

    // here we only resolve if there is no refersTo target yet. This reference may be the member of
    // a memberExpression
    // and therefore already resolved by resolveMember
    if (current instanceof DeclaredReferenceExpression) {
      DeclaredReferenceExpression ref = (DeclaredReferenceExpression) current;

      Set<Declaration> refersTo = new HashSet<>();
      refersTo.add(lang.getScopeManager().resolve(ref));
      refersTo.remove(null);

      Type recordDeclType = null;
      if (currentRecord != null) {
        recordDeclType = new Type(currentRecord.getName());
      }

      if (ref.getType().isFunctionPtr()
          && (refersTo.isEmpty()
              || refersTo.stream().anyMatch(FunctionDeclaration.class::isInstance))) {
        // If we already found something, this might either be a function pointer variable or a
        // function that would match the name. If we found a function, discard this finding, as
        // it is most likely not correct yet
        refersTo = resolveFunctionPtr(recordDeclType, ref);
      }

      RecordDeclaration record = resolveRecord(recordDeclType);
      // only add new nodes for non-static unknown
      if (refersTo.isEmpty()
          && !(current instanceof StaticReferenceExpression)
          && recordDeclType != null
          && record != null) {
        // Maybe we are referring to a field instead of a local var
        Set<Declaration> resolvedMember = new HashSet<>();
        resolvedMember.add(resolveMember(recordDeclType, (DeclaredReferenceExpression) current));
        refersTo = resolvedMember;
      }

      if (!refersTo.isEmpty()) {
        ref.setRefersTo(refersTo);
      } else {
        warnWithFileLocation(current, log, "Did not find a declaration for {}");
      }
    }
  }

  private void resolveFieldUsages(Node current, RecordDeclaration curClass) {

    if (current instanceof MemberExpression) {
      MemberExpression memberExpression = (MemberExpression) current;
      Node base = memberExpression.getBase();
      Node member = memberExpression.getMember();
      if (base instanceof DeclaredReferenceExpression) {
        base = resolveBase((DeclaredReferenceExpression) memberExpression.getBase());
      }
      if (member instanceof DeclaredReferenceExpression) {
        if (base instanceof EnumDeclaration) {
          String name = member.getName();
          member =
              ((EnumDeclaration) base)
                  .getEntries().stream()
                      .filter(e -> e.getName().equals(name))
                      .findFirst()
                      .orElse(null);
        } else {
          Type baseType = Type.getUnknown();
          if (base instanceof HasType) {
            baseType = ((HasType) base).getType();
          }
          if (base instanceof RecordDeclaration) {
            baseType = new Type(base.getName());
          }
          member =
              base == null
                  ? null
                  : resolveMember(
                      baseType, (DeclaredReferenceExpression) memberExpression.getMember());
          if (member != null) {
            HasType typedMember = (HasType) member;
            typedMember.setType(memberExpression.getType());
            Set<Type> subTypes = new HashSet<>(typedMember.getPossibleSubTypes());
            subTypes.addAll(memberExpression.getPossibleSubTypes());
            typedMember.setPossibleSubTypes(subTypes);
          }
        }
      }

      if (base != null && member != null) {
        // Not replacing the reference expressions but setting the reference to avoid connecting the
        // ast-subgraph
        if (member instanceof Declaration) {
          ((DeclaredReferenceExpression) memberExpression.getMember())
              .setRefersTo((Declaration) member);
        }
        if (base instanceof Declaration) {
          ((DeclaredReferenceExpression) memberExpression.getBase())
              .setRefersTo((Declaration) base);
        }
      } else {
        log.warn("Unexpected: null base or member in field usage: {}", current);
      }
    }
  }

  private Declaration resolveBase(DeclaredReferenceExpression reference) {

    ValueDeclaration declaration = lang.getScopeManager().resolve(reference);
    if (declaration != null) {
      return declaration;
    }

    // check if this refers to an enum
    if (enumMap.containsKey(reference.getType())) {
      return enumMap.get(reference.getType());
    }

    RecordDeclaration recordDeclaration = resolveRecord(reference.getType());
    if (recordDeclaration != null) {
      if (reference instanceof StaticReferenceExpression) {
        return recordDeclaration;
      } else {
        // check if we have this type as a class in our graph. If so, we can refer to its "this"
        // field
        if (recordDeclaration.getThis() != null) {
          return recordDeclaration.getThis();
        } else {
          return recordDeclaration;
        }
      }
    } else {
      log.info(
          "Type declaration for {} not found in graph, using dummy to collect all " + "usages",
          reference.getType());
      return handleUnknownField(reference.getType(), reference);
    }
  }

  private Declaration resolveMember(Type baseType, DeclaredReferenceExpression reference) {

    Optional<Declaration> member = Optional.empty();
    if (!TypeManager.getInstance().isUnknown(baseType)) {
      RecordDeclaration record = resolveRecord(baseType);
      if (record != null) {
        member =
            Optional.ofNullable(
                lang.getScopeManager().resolveInInheritanceHierarchy(record, reference));
      }
    }
    // Attention: using orElse instead of orElseGet will always invoke unknown declaration handling!
    return member.orElseGet(() -> handleUnknownField(baseType, reference));
  }

  private FieldDeclaration handleUnknownField(Type base, DeclaredReferenceExpression reference) {
    recordMap.putIfAbsent(
        base,
        NodeBuilder.newRecordDeclaration(
            base.getTypeName(),
            new ArrayList<>(),
            Type.UNKNOWN_TYPE_STRING,
            Type.UNKNOWN_TYPE_STRING));
    // fields.putIfAbsent(base, new ArrayList<>());
    List<FieldDeclaration> declarations = recordMap.get(base).getFields();
    Optional<FieldDeclaration> target =
        declarations.stream().filter(f -> f.getName().equals(reference.getName())).findFirst();
    if (target.isEmpty()) {
      FieldDeclaration declaration =
          NodeBuilder.newFieldDeclaration(
              reference.getName(), reference.getType(), Collections.emptyList(), "", null, null);
      // declarations.add(declaration);
      declaration.setImplicit(true);
      lang.getScopeManager().addDeclaration(declaration);
      return declaration;
    } else {
      return target.get();
    }
  }

  private MethodDeclaration handleUnknownClassMethod(Type base, String name, Type type) {
    recordMap.putIfAbsent(
        base,
        NodeBuilder.newRecordDeclaration(
            base.getTypeName(),
            new ArrayList<>(),
            Type.UNKNOWN_TYPE_STRING,
            Type.UNKNOWN_TYPE_STRING));
    RecordDeclaration containingRecord = recordMap.get(base);
    List<MethodDeclaration> declarations = containingRecord.getMethods();
    Optional<MethodDeclaration> target =
        declarations.stream().filter(f -> f.getName().equals(name)).findFirst();
    if (target.isEmpty()) {
      MethodDeclaration declaration =
          NodeBuilder.newMethodDeclaration(name, "", false, containingRecord);
      declaration.setType(type);
      // declarations.add(declaration);
      declaration.setImplicit(true);
      lang.getScopeManager().addDeclaration(declaration);
      return declaration;
    } else {
      return target.get();
    }
  }

  private FunctionDeclaration handleUnknownMethod(String name, Type type) {
    Optional<FunctionDeclaration> target =
        currTu.getDeclarations().stream()
            .filter(FunctionDeclaration.class::isInstance)
            .map(FunctionDeclaration.class::cast)
            .filter(f -> f.getName().equals(name))
            .findFirst();
    if (target.isEmpty()) {
      FunctionDeclaration declaration = NodeBuilder.newFunctionDeclaration(name, "");
      declaration.setType(type);
      currTu.getDeclarations().add(declaration);
      declaration.setImplicit(true);
      // Current TU should be the current method
      lang.getScopeManager().addDeclaration(declaration);
      return declaration;
    } else {
      return target.get();
    }
  }
}
