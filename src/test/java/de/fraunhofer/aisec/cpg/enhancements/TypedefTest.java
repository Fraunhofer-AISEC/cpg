package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypedefTest extends BaseTest {

  private final Path topLevel = Path.of("src", "test", "resources", "typedefs");

  @Test
  void testSingle() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel, true);
    List<ValueDeclaration> variables = TestUtils.subnodesOfType(result, ValueDeclaration.class);

    // normal type
    ValueDeclaration l1 = TestUtils.findByUniqueName(variables, "l1");
    ValueDeclaration l2 = TestUtils.findByUniqueName(variables, "l2");
    assertEquals(l1.getType(), l2.getType());

    // pointer
    ValueDeclaration longptr1 = TestUtils.findByUniqueName(variables, "longptr1");
    ValueDeclaration longptr2 = TestUtils.findByUniqueName(variables, "longptr2");
    assertEquals(longptr1.getType(), longptr2.getType());

    // array
    ValueDeclaration arr1 = TestUtils.findByUniqueName(variables, "arr1");
    ValueDeclaration arr2 = TestUtils.findByUniqueName(variables, "arr2");
    assertEquals(arr1.getType(), arr2.getType());

    // function pointer
    ValueDeclaration uintfp1 = TestUtils.findByUniqueName(variables, "uintfp1");
    ValueDeclaration uintfp2 = TestUtils.findByUniqueName(variables, "uintfp2");
    assertEquals(uintfp1.getType(), uintfp2.getType());
  }

  @Test
  void testWithModifier() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel, true);
    List<ValueDeclaration> variables = TestUtils.subnodesOfType(result, ValueDeclaration.class);

    // pointer
    ValueDeclaration l1ptr = TestUtils.findByUniqueName(variables, "l1ptr");
    ValueDeclaration l2ptr = TestUtils.findByUniqueName(variables, "l2ptr");
    ValueDeclaration l3ptr = TestUtils.findByUniqueName(variables, "l3ptr");
    ValueDeclaration l4ptr = TestUtils.findByUniqueName(variables, "l4ptr");
    assertEquals(l1ptr.getType(), l2ptr.getType());
    assertEquals(l1ptr.getType(), l3ptr.getType());
    assertEquals(l1ptr.getType(), l4ptr.getType());

    // arrays
    ValueDeclaration l1arr = TestUtils.findByUniqueName(variables, "l1arr");
    ValueDeclaration l2arr = TestUtils.findByUniqueName(variables, "l2arr");
    ValueDeclaration l3arr = TestUtils.findByUniqueName(variables, "l3arr");
    ValueDeclaration l4arr = TestUtils.findByUniqueName(variables, "l4arr");
    assertTrue(TypeManager.getInstance().checkArrayAndPointer(l1arr.getType(), l2arr.getType()));
    assertTrue(TypeManager.getInstance().checkArrayAndPointer(l1arr.getType(), l3arr.getType()));
    assertTrue(TypeManager.getInstance().checkArrayAndPointer(l1arr.getType(), l4arr.getType()));
  }

  @Test
  void testChained() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel, true);
    List<ValueDeclaration> variables = TestUtils.subnodesOfType(result, ValueDeclaration.class);
    ValueDeclaration l1 = TestUtils.findByUniqueName(variables, "l1");
    ValueDeclaration l3 = TestUtils.findByUniqueName(variables, "l3");
    ValueDeclaration l4 = TestUtils.findByUniqueName(variables, "l4");
    assertEquals(l1.getType(), l3.getType());
    assertEquals(l1.getType(), l4.getType());
  }

  @Test
  void testMultiple() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel, true);
    List<ValueDeclaration> variables = TestUtils.subnodesOfType(result, ValueDeclaration.class);

    // simple type
    ValueDeclaration i1 = TestUtils.findByUniqueName(variables, "i1");
    ValueDeclaration i2 = TestUtils.findByUniqueName(variables, "i2");
    assertEquals(i1.getType(), i2.getType());

    // array
    ValueDeclaration a1 = TestUtils.findByUniqueName(variables, "a1");
    ValueDeclaration a2 = TestUtils.findByUniqueName(variables, "a2");
    assertEquals(a1.getType(), a2.getType());

    // pointer
    ValueDeclaration intPtr1 = TestUtils.findByUniqueName(variables, "intPtr1");
    ValueDeclaration intPtr2 = TestUtils.findByUniqueName(variables, "intPtr2");
    assertEquals(intPtr1.getType(), intPtr2.getType());

    // function pointer
    ValueDeclaration fPtr1 = TestUtils.findByUniqueName(variables, "intFptr1");
    ValueDeclaration fPtr2 = TestUtils.findByUniqueName(variables, "intFptr2");
    assertEquals(fPtr1.getType(), fPtr2.getType());
  }

  @Test
  void testStructs() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel, true);
    List<ValueDeclaration> variables = TestUtils.subnodesOfType(result, ValueDeclaration.class);
    ValueDeclaration ps1 = TestUtils.findByUniqueName(variables, "ps1");
    ValueDeclaration ps2 = TestUtils.findByUniqueName(variables, "ps2");
    assertEquals(ps1.getType(), ps2.getType());
  }

  @Test
  void testArbitraryTypedefLocation() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel, true);
    List<ValueDeclaration> variables = TestUtils.subnodesOfType(result, ValueDeclaration.class);
    ValueDeclaration ullong1 = TestUtils.findByUniqueName(variables, "someUllong1");
    ValueDeclaration ullong2 = TestUtils.findByUniqueName(variables, "someUllong2");
    assertEquals(ullong1.getType(), ullong2.getType());
  }

  @Test
  void testMemberTypeDef() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel, true);
    List<ValueDeclaration> variables = TestUtils.subnodesOfType(result, ValueDeclaration.class);
    List<RecordDeclaration> records = TestUtils.subnodesOfType(result, RecordDeclaration.class);
    RecordDeclaration addConst = TestUtils.findByUniqueName(records, "add_const");
    ValueDeclaration typeMember1 = TestUtils.findByUniqueName(addConst.getFields(), "typeMember1");
    ValueDeclaration typeMember2 = TestUtils.findByUniqueName(addConst.getFields(), "typeMember2");
    assertEquals(typeMember1.getType(), typeMember2.getType());

    ValueDeclaration typeMemberOutside = TestUtils.findByUniqueName(variables, "typeMemberOutside");
    assertNotEquals(typeMemberOutside.getType(), typeMember2.getType());

    ValueDeclaration cptr1 = TestUtils.findByUniqueName(variables, "cptr1");
    ValueDeclaration cptr2 = TestUtils.findByUniqueName(variables, "cptr2");
    assertEquals(cptr1.getType(), cptr2.getType());
    assertNotEquals(typeMemberOutside.getType(), cptr2.getType());
  }
}
