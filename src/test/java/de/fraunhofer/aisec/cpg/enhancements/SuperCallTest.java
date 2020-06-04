package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SuperCallTest {

  private final Path topLevel = Path.of("src", "test", "resources", "superCalls");

  @Test
  void testSimpleCall() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel);
    List<RecordDeclaration> records = Util.subnodesOfType(result, RecordDeclaration.class);

    RecordDeclaration superClass = TestUtils.findByUniqueName(records, "SuperClass");
    List<MethodDeclaration> superMethods = Util.subnodesOfType(superClass, MethodDeclaration.class);
    MethodDeclaration superTarget = TestUtils.findByUniqueName(superMethods, "target");

    RecordDeclaration subClass = TestUtils.findByUniqueName(records, "SubClass");
    List<MethodDeclaration> methods = Util.subnodesOfType(subClass, MethodDeclaration.class);
    MethodDeclaration target = TestUtils.findByUniqueName(methods, "target");
    List<CallExpression> calls = Util.subnodesOfType(target, CallExpression.class);
    CallExpression superCall =
        TestUtils.findByPredicate(calls, c -> "super.target();".equals(c.getCode()));

    assertEquals(List.of(superTarget), superCall.getInvokes());
  }

  @Test
  void testInterfaceCall() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel);
    List<RecordDeclaration> records = Util.subnodesOfType(result, RecordDeclaration.class);

    RecordDeclaration interface1 = TestUtils.findByUniqueName(records, "Interface1");
    List<MethodDeclaration> interface1Methods =
        Util.subnodesOfType(interface1, MethodDeclaration.class);
    MethodDeclaration interface1Target = TestUtils.findByUniqueName(interface1Methods, "target");

    RecordDeclaration interface2 = TestUtils.findByUniqueName(records, "Interface2");
    List<MethodDeclaration> interface2Methods =
        Util.subnodesOfType(interface2, MethodDeclaration.class);
    MethodDeclaration interface2Target = TestUtils.findByUniqueName(interface2Methods, "target");

    RecordDeclaration subClass = TestUtils.findByUniqueName(records, "SubClass");
    List<MethodDeclaration> methods = Util.subnodesOfType(subClass, MethodDeclaration.class);
    MethodDeclaration target = TestUtils.findByUniqueName(methods, "target");
    List<CallExpression> calls = Util.subnodesOfType(target, CallExpression.class);
    CallExpression interface1Call =
        TestUtils.findByPredicate(calls, c -> "Interface1.super.target();".equals(c.getCode()));
    CallExpression interface2Call =
        TestUtils.findByPredicate(calls, c -> "Interface2.super.target();".equals(c.getCode()));

    assertEquals(List.of(interface1Target), interface1Call.getInvokes());
    assertEquals(List.of(interface2Target), interface2Call.getInvokes());
  }

  @Test
  void testSuperField() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel);
    List<RecordDeclaration> records = Util.subnodesOfType(result, RecordDeclaration.class);

    RecordDeclaration superClass = TestUtils.findByUniqueName(records, "SuperClass");
    FieldDeclaration superField = TestUtils.findByUniqueName(superClass.getFields(), "field");

    RecordDeclaration subClass = TestUtils.findByUniqueName(records, "SubClass");
    List<MethodDeclaration> methods = Util.subnodesOfType(subClass, MethodDeclaration.class);
    FieldDeclaration field = TestUtils.findByUniqueName(subClass.getFields(), "field");

    MethodDeclaration getField = TestUtils.findByUniqueName(methods, "getField");
    List<MemberExpression> refs = Util.subnodesOfType(getField, MemberExpression.class);
    MemberExpression fieldRef = TestUtils.findByPredicate(refs, r -> "field".equals(r.getCode()));

    MethodDeclaration getSuperField = TestUtils.findByUniqueName(methods, "getSuperField");
    refs = Util.subnodesOfType(getSuperField, MemberExpression.class);
    MemberExpression superFieldRef =
        TestUtils.findByPredicate(refs, r -> "super.field".equals(r.getCode()));

    assertEquals(subClass.getThis(), fieldRef.getBase());
    assertEquals(field, fieldRef.getMember());
    assertEquals(superClass.getThis(), superFieldRef.getBase());
    assertEquals(superField, superFieldRef.getMember());
  }

  @Test
  void testInnerCall() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel);
    List<RecordDeclaration> records = Util.subnodesOfType(result, RecordDeclaration.class);

    RecordDeclaration superClass = TestUtils.findByUniqueName(records, "SuperClass");
    List<MethodDeclaration> superMethods = Util.subnodesOfType(superClass, MethodDeclaration.class);
    MethodDeclaration superTarget = TestUtils.findByUniqueName(superMethods, "target");

    RecordDeclaration innerClass = TestUtils.findByUniqueName(records, "SubClass.Inner");
    List<MethodDeclaration> methods = Util.subnodesOfType(innerClass, MethodDeclaration.class);
    MethodDeclaration target = TestUtils.findByUniqueName(methods, "inner");
    List<CallExpression> calls = Util.subnodesOfType(target, CallExpression.class);
    CallExpression superCall =
        TestUtils.findByPredicate(calls, c -> "SubClass.super.target();".equals(c.getCode()));

    assertEquals(List.of(superTarget), superCall.getInvokes());
  }
}
