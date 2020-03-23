package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TypedefTest {

  private Path topLevel = Path.of("src", "test", "resources", "typedefs");

  @Test
  void testSingle() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<ValueDeclaration> variables = Util.subnodesOfType(result, ValueDeclaration.class);

    // normal type
    ValueDeclaration l1 = TestUtils.findByName(variables, "l1");
    ValueDeclaration l2 = TestUtils.findByName(variables, "l2");
    assertEquals(l1.getType(), l2.getType());

    // pointer
    ValueDeclaration longptr1 = TestUtils.findByName(variables, "longptr1");
    ValueDeclaration longptr2 = TestUtils.findByName(variables, "longptr2");
    assertEquals(longptr1.getType(), longptr2.getType());

    // array
    ValueDeclaration arr1 = TestUtils.findByName(variables, "arr1");
    ValueDeclaration arr2 = TestUtils.findByName(variables, "arr2");
    assertEquals(arr1.getType(), arr2.getType());

    // function pointer
    ValueDeclaration uintfp1 = TestUtils.findByName(variables, "uintfp1");
    ValueDeclaration uintfp2 = TestUtils.findByName(variables, "uintfp2");
    assertEquals(uintfp1.getType(), uintfp2.getType());
  }

  @Test
  void testChained() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<ValueDeclaration> variables = Util.subnodesOfType(result, ValueDeclaration.class);
    ValueDeclaration l1 = TestUtils.findByName(variables, "l1");
    ValueDeclaration l3 = TestUtils.findByName(variables, "l3");
    ValueDeclaration l4 = TestUtils.findByName(variables, "l4");
    assertEquals(l1.getType(), l3.getType());
    assertEquals(l1.getType(), l4.getType());
  }

  @Test
  void testMultiple() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<ValueDeclaration> variables = Util.subnodesOfType(result, ValueDeclaration.class);

    // simple type
    ValueDeclaration i1 = TestUtils.findByName(variables, "i1");
    ValueDeclaration i2 = TestUtils.findByName(variables, "i2");
    assertEquals(i1.getType(), i2.getType());

    // array
    ValueDeclaration a1 = TestUtils.findByName(variables, "a1");
    ValueDeclaration a2 = TestUtils.findByName(variables, "a2");
    assertEquals(a1.getType(), a2.getType());

    // pointer
    ValueDeclaration intPtr1 = TestUtils.findByName(variables, "intPtr1");
    ValueDeclaration intPtr2 = TestUtils.findByName(variables, "intPtr2");
    assertEquals(intPtr1.getType(), intPtr2.getType());

    // function pointer
    ValueDeclaration fPtr1 = TestUtils.findByName(variables, "intFptr1");
    ValueDeclaration fPtr2 = TestUtils.findByName(variables, "intFptr2");
    assertEquals(fPtr1.getType(), fPtr2.getType());
  }

  @Test
  void testStructs() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<ValueDeclaration> variables = Util.subnodesOfType(result, ValueDeclaration.class);
    ValueDeclaration ps1 = TestUtils.findByName(variables, "ps1");
    ValueDeclaration ps2 = TestUtils.findByName(variables, "ps2");
    assertEquals(ps1.getType(), ps2.getType());
  }

  @Test
  void testArbitraryTypedefLocation() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<ValueDeclaration> variables = Util.subnodesOfType(result, ValueDeclaration.class);
    ValueDeclaration ullong1 = TestUtils.findByName(variables, "someUllong1");
    ValueDeclaration ullong2 = TestUtils.findByName(variables, "someUllong2");
    assertEquals(ullong1.getType(), ullong2.getType());
  }

  @Test
  void testMemberTypeDef() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<ValueDeclaration> variables = Util.subnodesOfType(result, ValueDeclaration.class);
    List<RecordDeclaration> records = Util.subnodesOfType(result, RecordDeclaration.class);
    RecordDeclaration addConst = TestUtils.findByName(records, "add_const");
    ValueDeclaration typeMember1 = TestUtils.findByName(addConst.getFields(), "typeMember1");
    ValueDeclaration typeMember2 = TestUtils.findByName(addConst.getFields(), "typeMember2");
    assertEquals(typeMember1.getType(), typeMember2.getType());

    ValueDeclaration typeMemberOutside = TestUtils.findByName(variables, "typeMemberOutside");
    assertNotEquals(typeMemberOutside.getType(), typeMember2.getType());

    ValueDeclaration cptr1 = TestUtils.findByName(variables, "cptr1");
    ValueDeclaration cptr2 = TestUtils.findByName(variables, "cptr2");
    assertEquals(cptr1.getType(), cptr2.getType());
    assertNotEquals(typeMemberOutside.getType(), cptr2.getType());
  }
}
