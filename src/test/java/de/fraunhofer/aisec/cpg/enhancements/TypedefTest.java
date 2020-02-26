package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TypedefTest {

  private Path topLevel = Path.of("src", "test", "resources", "typedefs");

  @Test
  void testSimple() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<VariableDeclaration> variables = Util.subnodesOfType(result, VariableDeclaration.class);
    VariableDeclaration l1 = TestUtils.findByName(variables, "l1");
    VariableDeclaration l2 = TestUtils.findByName(variables, "l2");
    VariableDeclaration i1 = TestUtils.findByName(variables, "i1");
    VariableDeclaration i2 = TestUtils.findByName(variables, "i2");
    assertEquals(l1.getType(), l2.getType());
    assertEquals(i1.getType(), i2.getType());
  }

  @Test
  void testChained() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<VariableDeclaration> variables = Util.subnodesOfType(result, VariableDeclaration.class);
    VariableDeclaration l1 = TestUtils.findByName(variables, "l1");
    VariableDeclaration l3 = TestUtils.findByName(variables, "l3");
    VariableDeclaration l4 = TestUtils.findByName(variables, "l4");
    assertEquals(l1.getType(), l3.getType());
    assertEquals(l1.getType(), l4.getType());
  }

  @Test
  void testComplex() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<VariableDeclaration> variables = Util.subnodesOfType(result, VariableDeclaration.class);

    // array
    VariableDeclaration a1 = TestUtils.findByName(variables, "a1");
    VariableDeclaration a2 = TestUtils.findByName(variables, "a2");
    assertEquals(a1.getType(), a2.getType());

    // pointer
    VariableDeclaration intPtr1 = TestUtils.findByName(variables, "intPtr1");
    VariableDeclaration intPtr2 = TestUtils.findByName(variables, "intPtr2");
    assertEquals(intPtr1.getType(), intPtr2.getType());

    // function pointer
    VariableDeclaration fPtr1 = TestUtils.findByName(variables, "intFptr1");
    VariableDeclaration fPtr2 = TestUtils.findByName(variables, "intFptr2");
    assertEquals(fPtr1.getType(), fPtr2.getType());
  }

  @Test
  void testStructs() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<VariableDeclaration> variables = Util.subnodesOfType(result, VariableDeclaration.class);
    VariableDeclaration ps1 = TestUtils.findByName(variables, "ps1");
    VariableDeclaration ps2 = TestUtils.findByName(variables, "ps2");
    assertEquals(ps1.getType(), ps2.getType());
  }

  @Test
  void testArbitraryTypedefLocation() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<VariableDeclaration> variables = Util.subnodesOfType(result, VariableDeclaration.class);
    VariableDeclaration ullong1 = TestUtils.findByName(variables, "someUllong1");
    VariableDeclaration ullong2 = TestUtils.findByName(variables, "someUllong2");
    assertEquals(ullong1.getType(), ullong2.getType());
  }

  @Test
  void testMemberTypeDef() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel);
    List<VariableDeclaration> variables = Util.subnodesOfType(result, VariableDeclaration.class);
    List<RecordDeclaration> records = Util.subnodesOfType(result, RecordDeclaration.class);
    RecordDeclaration addConst = TestUtils.findByName(records, "add_const");
    FieldDeclaration typeMember1 = TestUtils.findByName(addConst.getFields(), "typeMember1");
    FieldDeclaration typeMember2 = TestUtils.findByName(addConst.getFields(), "typeMember2");
    assertEquals(typeMember1.getType(), typeMember2.getType());

    VariableDeclaration typeMemberOutside = TestUtils.findByName(variables, "typeMemberOutside");
    assertNotEquals(typeMemberOutside.getType(), typeMember2.getType());
  }
}
