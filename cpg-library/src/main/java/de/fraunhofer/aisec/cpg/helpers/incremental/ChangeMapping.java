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
package de.fraunhofer.aisec.cpg.helpers.incremental;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.util.*;
import java.util.stream.Collectors;

public class ChangeMapping {

  public static boolean processChanges(
      List<TranslationUnitDeclaration> originalTUs, List<TranslationUnitDeclaration> changedTUs) {
    boolean passesNotNeeded = true;
    for (TranslationUnitDeclaration changed : changedTUs) {
      TranslationUnitDeclaration original =
          originalTUs.stream()
              .filter(t -> t.getName().equals(changed.getName()))
              .findFirst()
              .orElse(null);
      if (original == changed) {
        continue;
      }
      passesNotNeeded &= processTU(original, changed);
    }
    return passesNotNeeded;
  }

  private static boolean processTU(
      TranslationUnitDeclaration original, TranslationUnitDeclaration changed) {
    if (original == null
        || changed == null
        || !original.getAllPropertiesToCompare().equals(changed.getAllPropertiesToCompare())) {
      return false;
    }

    boolean passesNotNeeded = true;
    Deque<WorkItem> todo = new ArrayDeque<>();
    Deque<WorkItem> backlog = new ArrayDeque<>();
    Set<Node> seen = Collections.newSetFromMap(new IdentityHashMap<>());

    todo.push(new WorkItem(original, changed));
    while (!todo.isEmpty()) {
      WorkItem current = todo.pop();
      seen.add(current.getOriginal());
      seen.add(current.getChanged());
      if (!backlog.isEmpty() && backlog.peek() == current) {
        backlog.pop();
      } else {
        // re-place current node as marker for scope exit
        todo.push(current);
        backlog.push(current);

        if (current.isInstance(FieldDeclaration.class)) {
          passesNotNeeded &=
              handleField(
                  (FieldDeclaration) current.getOriginal(),
                  (FieldDeclaration) current.getChanged());
        } else if (current.isInstance(FunctionDeclaration.class)) {
          passesNotNeeded &=
              handleFunction(
                  (FunctionDeclaration) current.getOriginal(),
                  (FunctionDeclaration) current.getChanged());
        } else {
          if (!tryMapChildren(todo, seen, current.getOriginal(), current.getChanged())) {
            // Some children didn't match, give up
            return false;
          }
        }
      }
    }

    return passesNotNeeded;
  }

  private static boolean tryMapChildren(
      Deque<WorkItem> todo, Set<Node> seen, Node currOriginal, Node currChanged) {
    List<Node> originalChildren =
        SubgraphWalker.getAstChildren(currOriginal).stream()
            .filter(c -> !seen.contains(c))
            .collect(Collectors.toList());
    List<Node> changedChildren =
        SubgraphWalker.getAstChildren(currChanged).stream()
            .filter(c -> !seen.contains(c))
            .collect(Collectors.toList());

    boolean childrenMatch = true;
    List<Map<String, Object>> propertiesOriginalChildren =
        originalChildren.stream().map(Node::getAllPropertiesToCompare).collect(Collectors.toList());
    List<Map<String, Object>> propertiesChangedChildren =
        changedChildren.stream().map(Node::getAllPropertiesToCompare).collect(Collectors.toList());

    for (int i = 0; i < originalChildren.size(); i++) {
      Node originalChild = originalChildren.get(i);
      Map<String, Object> propertiesOriginalChild = propertiesOriginalChildren.get(i);

      boolean matchFound = false;
      int j;
      for (j = 0; j < changedChildren.size(); j++) {
        if (propertiesChangedChildren.get(j).equals(propertiesOriginalChild)) {
          matchFound = true;
          Node replacement = changedChildren.get(j);
          todo.push(new WorkItem(originalChild, replacement));
          break;
        }
      }
      if (matchFound) {
        changedChildren.remove(j);
        propertiesChangedChildren.remove(j);
      } else {
        if (originalChild instanceof FieldDeclaration
            || originalChild instanceof FunctionDeclaration) {
          todo.push(new WorkItem(originalChild, null));
        } else {
          childrenMatch = false;
        }
      }
    }

    for (Node changedChild : changedChildren) {
      if (changedChild instanceof FieldDeclaration || changedChild instanceof FunctionDeclaration) {
        todo.push(new WorkItem(null, changedChild));
      } else {
        childrenMatch = false;
      }
    }

    return childrenMatch;
  }

  private static boolean handleField(FieldDeclaration original, FieldDeclaration changed) {
    if (original != null) {
      original.setUnknownType();
    } else {
      // TODO Field has been added
      return false;
    }

    if (changed == null) {
      // Field has been deleted, clear refersTo and propagate unknown type
      for (DeclaredReferenceExpression usedBy : original.getIncomingReferences()) {
        usedBy.setRefersTo(null);
        usedBy.setUnknownType();
      }
    } else {
      // Field replaces old one
      for (DeclaredReferenceExpression usedBy : original.getIncomingReferences()) {
        usedBy.setRefersTo(changed);
      }
    }

    return true;
  }

  private static boolean handleFunction(FunctionDeclaration original, FunctionDeclaration changed) {
    if (original != null) {
      original.setUnknownType();
    } else {
      // TODO Function has been added
      return false;
    }

    if (original instanceof ConstructorDeclaration && changed instanceof ConstructorDeclaration) {
      handleConstructor((ConstructorDeclaration) original, (ConstructorDeclaration) changed);
    }

    if (changed == null) {
      // Function has been deleted, clear invokes
      for (CallExpression caller : original.getIncomingInvokes()) {
        List<FunctionDeclaration> newInvokes =
            caller.getInvokes().stream().filter(i -> i != original).collect(Collectors.toList());
        caller.setInvokes(newInvokes);
      }
    } else {
      // Function replaces old one
      if (original.hasSignature(changed.getSignatureTypes())) {
        // Transfer all invokes and replace original by changed
        for (CallExpression caller : original.getIncomingInvokes()) {
          List<FunctionDeclaration> newInvokes =
              caller.getInvokes().stream()
                  .filter(i -> i != original)
                  .collect(Collectors.toCollection(ArrayList::new));
          newInvokes.add(changed);
          caller.setInvokes(newInvokes);
        }
      } else {
        // Remove original from invokes but don't add changed
        for (CallExpression caller : original.getIncomingInvokes()) {
          List<FunctionDeclaration> newInvokes =
              caller.getInvokes().stream().filter(i -> i != original).collect(Collectors.toList());
          caller.setInvokes(newInvokes);
        }
      }
    }

    return true;
  }

  private static void handleConstructor(
      ConstructorDeclaration original, ConstructorDeclaration changed) {
    if (changed == null) {
      // Constructor has been deleted, clear invokes
      for (ConstructExpression caller : original.getIncomingConstructorCalls()) {
        caller.setConstructor(null);
        caller.setInstantiates(null);
      }
    } else {
      // Constructor replaces old one
      if (original.hasSignature(changed.getSignatureTypes())) {
        for (ConstructExpression caller : original.getIncomingConstructorCalls()) {
          caller.setConstructor(changed);
          caller.setInstantiates(changed.getRecordDeclaration());
        }
      } else {
        // Remove original from invokes but don't add changed
        for (ConstructExpression caller : original.getIncomingConstructorCalls()) {
          caller.setConstructor(null);
          caller.setInstantiates(null);
        }
      }
    }
  }

  private static class WorkItem {
    private final Node original;
    private final Node changed;

    public Node getOriginal() {
      return original;
    }

    public Node getChanged() {
      return changed;
    }

    public boolean isInstance(Class<?> toCheck) {
      return toCheck.isInstance(original) || toCheck.isInstance(changed);
    }

    public WorkItem(Node original, Node changed) {
      this.original = original;
      this.changed = changed;
    }

    @Override
    public String toString() {
      return "Original: " + original + ", changed: " + changed;
    }
  }
}
