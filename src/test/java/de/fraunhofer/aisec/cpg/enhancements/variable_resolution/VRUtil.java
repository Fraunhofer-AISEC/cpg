package de.fraunhofer.aisec.cpg.enhancements.variable_resolution;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.MemberExpression;
import de.fraunhofer.aisec.cpg.graph.Node;
import java.util.Collection;

/**
 * Utility-class to bundle functionality for the both test classes {@link VariableResolverCppTest}
 * and {@link VariableResolverJavaTest}.
 */
public class VRUtil {

  /** Used to flexibly switch between refers to as a collection or a single element */
  public static boolean REFERES_TO_IS_A_COLLECTION = true;
  /**
   * Currently when variables or fields are used they can be stored in the expression itself, in the
   * future, a reference as usage indicator pointing to the used ValueDeclaration is planed.
   */
  public static boolean ENFORCE_REFERENCES = false;
  /**
   * Currently there is no unified enforced structure when using fields, this field is used set
   * whether or not the tests enforce the presence of a member expression
   */
  public static boolean ENFORCE_MEMBER_EXPRESSION = false;

  /**
   * Asserts equality or containing of the expected usedNode in the usingNode. If {@link
   * VRUtil#ENFORCE_REFERENCES} is true, {@code usingNode} must be a {@link
   * DeclaredReferenceExpression} where {@link DeclaredReferenceExpression#refersTo} is or contains
   * {@code usedNode}. If this is not the case, usage can also be interpreted as equality of the
   * two.
   *
   * @param usingNode - The node that shows usage of another node.
   * @param usedNode - The node that is expected to be used.
   */
  public static void assertUsageOf(Node usingNode, Node usedNode) {
    assertNotNull(usingNode);
    if (!(usingNode instanceof DeclaredReferenceExpression) && !ENFORCE_REFERENCES) {
      assertSame(usedNode, usingNode);
    } else {
      assertTrue(usingNode instanceof DeclaredReferenceExpression);
      DeclaredReferenceExpression reference = (DeclaredReferenceExpression) usingNode;
      assertSameOrContains(reference.getRefersTo(), usedNode);
    }
  }

  /**
   * Asserts that {@code usingNode} uses/references the provided {@code usedBase} and {@code
   * usedMember}. If {@link VRUtil#ENFORCE_MEMBER_EXPRESSION} is true, {@code usingNode} must be a
   * {@link MemberExpression} where {@link MemberExpression#base} uses {@code usedBase} and {@link
   * MemberExpression#member} uses {@code usedMember}. Using is checked as preformed per {@link
   * VRUtil#assertUsageOf(Node,Node)}
   *
   * @param usingNode - Node that uses some member
   * @param usedBase - The expected base that is used
   * @param usedMember - THe expected member that is used
   */
  public static void assertUsageOfMemberAndBase(Node usingNode, Node usedBase, Node usedMember) {
    assertNotNull(usingNode);
    if (!(usingNode instanceof MemberExpression) && !ENFORCE_MEMBER_EXPRESSION) {
      // Assumtion here is that the target of the member portion of the expression and not the base
      // is resolved
      assertUsageOf(usingNode, usedMember);
    } else {
      assertTrue(usingNode instanceof MemberExpression);
      MemberExpression memberExpression = (MemberExpression) usingNode;
      Node base = memberExpression.getBase();
      Node member = memberExpression.getMember();
      assertUsageOf(base, usedBase);
      assertUsageOf(member, usedMember);
    }
  }

  public static void assertSameOrContains(Object potentialCollection, Object toCompair) {
    if (REFERES_TO_IS_A_COLLECTION && potentialCollection instanceof Collection) {
      Collection collection = (Collection) potentialCollection;
      assertTrue(collection.stream().anyMatch(obj -> obj == toCompair));
    } else {
      assertSame(potentialCollection, toCompair);
    }
  }
}
