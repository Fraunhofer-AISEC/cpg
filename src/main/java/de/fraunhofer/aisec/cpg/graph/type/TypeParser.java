package de.fraunhofer.aisec.cpg.graph.type;

import de.fraunhofer.aisec.cpg.graph.TypeManager;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypeParser {

  public static final String UNKNOWN_TYPE_STRING = "UNKNOWN";
  private static final List<String> primitives =
      List.of("byte", "short", "int", "long", "float", "double", "boolean", "char");
  private static final Pattern TYPE_FROM_STRING =
      Pattern.compile(
          "(?:(?<modifier>[a-zA-Z]*) )?(?<type>[a-zA-Z0-9_$.<>]*)(?<adjustment>[\\[\\]*&\\s]*)?");
  private static final Pattern functionPtrRegex =
      Pattern.compile(
          "(?:(?<functionptr>(\\h|\\()+[a-zA-Z0-9_$.<>:]*\\*\\h*[a-zA-Z0-9_$.<>:]*(\\h|\\))+)\\h*)(?<args>\\(+[a-zA-Z0-9_$.<>,\\h]*\\))");

  private static String clean(String type) {
    if (type.contains("?")
        || type.contains("org.eclipse.cdt.internal.core.dom.parser.ProblemType@")) {
      return UNKNOWN_TYPE_STRING;
    }
    type = type.replaceAll("^struct ", "");
    type = type.replaceAll("^const struct ", "");
    type = type.replaceAll(" const ", " ");
    // remove artifacts from unidentified C++ namespaces
    type = type.replaceAll("\\{.*}::", "");
    // remove irrelevant array sizes cluttering the type name
    type = type.replaceAll("\\[[ \\d]*]", "[]");
    // remove function signature info
    type = type.replaceAll("\\(.*\\)", "");
    // unify separator
    type = type.replace("::", ".");
    return type.strip();
  }

  private static Type.Qualifier calcQualifier(List<String> typeString, Type.Qualifier old) {
    boolean constant_flag = false;
    boolean volatile_flag = false;
    boolean restrict_flag = false;
    boolean atomic_flag = false;

    if (old != null) {
      constant_flag = old.isConst();
      volatile_flag = old.isVolatile();
      restrict_flag = old.isRestrict();
      atomic_flag = old.isAtomic();
    }

    for (String part : typeString) {
      switch (part) {
        case "final":
        case "const":
          constant_flag = true;
          break;

        case "volatile":
          volatile_flag = true;
          break;

        case "restrict":
          restrict_flag = true;
          break;

        case "atomic":
          atomic_flag = true;
          break;
      }
    }

    return new Type.Qualifier(constant_flag, volatile_flag, restrict_flag, atomic_flag);
  }

  private static Type.Storage calcStorage(List<String> typeString) {
    for (String part : typeString) {
      try {
        return Type.Storage.valueOf(part.toUpperCase());
      } catch (IllegalArgumentException e) {
        continue;
      }
    }
    return Type.Storage.AUTO;
  }

  protected static boolean isStorageSpecifier(String specifier) {
    if (TypeManager.getInstance().getLanguage() == TypeManager.Language.CXX) {
      return specifier.toUpperCase().equals("STATIC");
    } else {
      try {
        Type.Storage.valueOf(specifier.toUpperCase());
        return true;
      } catch (IllegalArgumentException e) {
        return false;
      }
    }
  }

  protected static boolean isQualifierSpecifier(String qualifier) {
    if (TypeManager.getInstance().getLanguage() == TypeManager.Language.JAVA) {
      return qualifier.equals("final") || qualifier.equals("volatile");
    } else {
      return qualifier.equals("const")
          || qualifier.equals("volatile")
          || qualifier.equals("restrict")
          || qualifier.equals("atomic");
    }
  }

  private static boolean isKnownSpecifier(String specifier) {
    return isQualifierSpecifier(specifier) || isStorageSpecifier(specifier);
  }

  /**
   * searching closing bracket
   *
   * @param openBracket opening bracket char
   * @param closeBracket closing bracket char
   * @param string substring without the openening bracket
   * @return position of the closing bracket
   */
  private static int findMatching(char openBracket, char closeBracket, String string) {
    int counter = 1;
    int i = 0;

    while (counter != 0) {
      char actualChar = string.charAt(i);
      if (actualChar == openBracket) {
        counter++;
      } else if (actualChar == closeBracket) {
        counter--;
      }
      i++;
    }

    return i;
  }

  private static Matcher isFunctionPointer(List<String> type) {

    StringBuilder typeStringBuilder = new StringBuilder();
    for (String typePart : type) {
      typeStringBuilder.append(typePart);
    }

    String typeString = typeStringBuilder.toString().strip();

    Matcher matcher = functionPtrRegex.matcher(typeString);
    if (matcher.find()) {
      return matcher;
    }
    return null;
  }

  private static boolean isIncompleteType(String typeName) {
    return typeName.strip().equals("void");
  }

  private static boolean isUnknownType(String typeName) {
    return typeName.toUpperCase().contains("UNKNOWN");
  }

  private static List<String> fix(String type) {

    type = type.replace("::", ".");
    type = type.split("=")[0];

    // Guarantee that there is no arbitraty number of whitespces
    String[] typeSubpart = type.split(" ");

    StringBuilder typeBuilder = new StringBuilder();
    for (String part : typeSubpart) {
      if (!part.equals("")) {
        typeBuilder.append(part).append(" ");
      }
    }
    type = typeBuilder.toString().strip();
    List<String> typeBlocks = new ArrayList<>();

    String out = "";

    int lastSplit = 0;
    int finishPosition = 0;
    String substr = "";

    for (int i = 0; i < type.length(); i++) {
      char ch = type.charAt(i);
      switch (ch) {
        case ' ':
          // handle space create element
          substr = type.substring(lastSplit, i);
          if (substr.length() != 0) {
            typeBlocks.add(substr);
          }
          lastSplit = i + 1;
          break;

        case '(':
          // handle ( find matching closing ignore content
          substr = type.substring(lastSplit, i);
          if (substr.length() != 0) {
            typeBlocks.add(substr);
          }
          finishPosition = findMatching('(', ')', type.substring(i + 1));
          typeBlocks.add(type.substring(i, i + finishPosition + 1));
          i = finishPosition + i;
          lastSplit = i + 1;
          break;

        case '[':
          // handle [ find matching closing ignore content
          substr = type.substring(lastSplit, i);
          if (substr.length() != 0) {
            typeBlocks.add(substr);
          }

          finishPosition = findMatching('[', ']', type.substring(i + 1));
          typeBlocks.add("[]"); // type.substring(i, i+finishPosition+1)
          i = finishPosition + i;
          lastSplit = i + 1;
          break;

        case '*':
          // handle *
          substr = type.substring(lastSplit, i);
          if (substr.length() != 0) {
            typeBlocks.add(substr);
          }

          typeBlocks.add("*");
          lastSplit = i + 1;
          break;

        case '&':
          substr = type.substring(lastSplit, i);
          if (substr.length() != 0) {
            typeBlocks.add(substr);
          }

          typeBlocks.add("&");
          lastSplit = i + 1;
          break;

        default:
          // everything else
          substr = type.substring(lastSplit, type.length());
          if (substr.length() != 0 && i == type.length() - 1) {
            typeBlocks.add(substr);
          }
          break;
      }
    }

    return typeBlocks;
  }

  private static List<Type> getParameterList(String parameterList) {
    if (parameterList.startsWith("(") && parameterList.endsWith(")")) {
      parameterList = parameterList.strip().substring(1, parameterList.strip().length() - 1);
    }
    List<Type> parameters = new ArrayList<>();
    String[] parametersSplit = parameterList.split(",");
    for (String parameter : parametersSplit) {
      parameters.add(createFrom(parameter.strip()));
    }

    return parameters;
  }

  private static List<Type> getGenerics(String typeName) {
    if (typeName.contains("<") && typeName.contains(">")) {
      String generics = typeName.substring(typeName.indexOf('<') + 1, typeName.lastIndexOf('>'));
      List<Type> genericList = new ArrayList<>();
      String[] parametersSplit = generics.split(",");
      for (String parameter : parametersSplit) {
        genericList.add(createFrom(parameter.strip()));
      }

      return genericList;
    }
    return new ArrayList<>();
  }

  private static Type resolveBracketExpression(Type finalType, List<String> bracketExpressions) {
    for (String bracketExpression : bracketExpressions) {
      List<String> splitExpression =
          fix(bracketExpression.substring(1, bracketExpression.length() - 1));
      for (String part : splitExpression) {
        if (part.equals("*")) {
          finalType = finalType.reference();
        }

        if (part.equals("&")) {
          finalType = finalType.dereference();
        }

        if (part.startsWith("[") && part.endsWith("]")) {
          finalType = finalType.reference();
        }
        if (part.startsWith("(") && part.endsWith(")")) {
          List<String> subBracketExpression = new ArrayList<>();
          subBracketExpression.add(part);
          finalType = resolveBracketExpression(finalType, subBracketExpression);
        }
        if (isKnownSpecifier(part)) {
          if (isStorageSpecifier(part)) {
            List<String> specifiers = new ArrayList<>();
            specifiers.add(part);
            finalType.setStorage(calcStorage(specifiers));
          } else {
            List<String> qualifiers = new ArrayList<>();
            qualifiers.add(part);
            finalType.setQualifier(calcQualifier(qualifiers, finalType.getQualifier()));
          }
        }
      }
      return finalType;
    }

    return finalType;
  }

  private static String clear(String type) {
    return type.replaceAll("public|private|protected", "").strip();
  }

  private static boolean isPrimitiveType(List<String> stringList) {
    for (String s : stringList) {
      if (primitives.contains(s)) {
        return true;
      }
    }
    return false;
  }

  private static List<String> joinPrimitive(List<String> typeBlocks) {
    List<String> joinedTypeBlocks = new ArrayList<>();
    StringBuilder primitiveType = new StringBuilder();
    boolean foundPrimitive = false;

    for (String s : typeBlocks) {
      if (primitives.contains(s)) {
        if (primitiveType.length() > 0) {
          primitiveType.append(" ");
        }
        primitiveType.append(s);
      }
    }

    for (String s : typeBlocks) {
      if (primitives.contains(s) && !foundPrimitive) {
        joinedTypeBlocks.add(primitiveType.toString());
        foundPrimitive = true;
      } else {
        if (!primitives.contains(s)) {
          joinedTypeBlocks.add(s);
        }
      }
    }

    return joinedTypeBlocks;
  }

  public static Type createFrom(String type) {
    if (type.contains("?")
        || type.contains("org.eclipse.cdt.internal.core.dom.parser.ProblemType@")) {
      return UnknownType.getUnknownType();
    }
    if (type.length() == 0) {
      return UnknownType.getUnknownType();
    }
    type = clear(type);
    List<String> typeBlocks = fix(type);

    boolean primitiveType = isPrimitiveType(typeBlocks);

    ObjectType.Modifier modifier = ObjectType.Modifier.NOT_APPLICABLE;
    if (primitiveType) {
      if (typeBlocks.contains("unsigned")) {
        modifier = ObjectType.Modifier.UNSIGNED;
        typeBlocks.remove("unsigned");
      } else {
        modifier = ObjectType.Modifier.SIGNED;
        typeBlocks.remove("signed");
      }
    }

    typeBlocks = joinPrimitive(typeBlocks);

    List<String> qualifierList = new ArrayList<>();
    List<String> storageList = new ArrayList<>();

    int counter = 0;
    for (String part : typeBlocks) {
      if (isKnownSpecifier(part)) {
        if (isQualifierSpecifier(part)) {
          qualifierList.add(part);
        } else if (isStorageSpecifier(part)) {
          storageList.add(part);
        }
        counter++;
      } else {
        break;
      }
    }

    Type.Storage storageValue = calcStorage(storageList);
    Type.Qualifier qualifier = calcQualifier(qualifierList, null);

    String typeName = typeBlocks.get(counter);
    counter++;

    Type finalType;

    Matcher funcptr = isFunctionPointer(typeBlocks.subList(counter, typeBlocks.size()));

    if (funcptr != null) {
      Type returnType = createFrom(typeName);
      List<Type> parameterList = getParameterList(funcptr.group("args"));
      // TODO CPS into typeState in typeManager
      return new FunctionPointerType(qualifier, storageValue, parameterList, returnType);
    } else {
      if (isIncompleteType(typeName)) {
        // IncompleteType
        finalType = new IncompleteType();
      } else if (isUnknownType(typeName)) {
        finalType = new UnknownType(typeName);
      } else {
        // ObjectType
        List<Type> generics = getGenerics(typeName);
        if (typeName.contains("<") && typeName.contains(">")) {
          typeName = typeName.substring(0, typeName.indexOf("<"));
        }
        finalType =
            new ObjectType(typeName, storageValue, qualifier, generics, modifier, primitiveType);
        if (finalType.getTypeName().equals("auto")) {
          return UnknownType.getUnknownType();
        }
      }
    }

    List<String> subPart = typeBlocks.subList(counter, typeBlocks.size());

    List<String> bracketExpressions = new ArrayList<>();

    for (String part : subPart) {

      if (part.equals("*")) {
        finalType = finalType.reference();
      }

      if (part.equals("&")) {
        Type.Qualifier oldQualifier = finalType.getQualifier();
        Type.Storage oldStorage = finalType.getStorage();
        finalType.setQualifier(new Type.Qualifier());
        finalType.setStorage(Type.Storage.AUTO);
        finalType = new ReferenceType(finalType);
        finalType.setStorage(oldStorage);
        finalType.setQualifier(oldQualifier);
      }

      if (part.startsWith("[") && part.endsWith("]")) {
        finalType = finalType.reference();
      }

      if (part.startsWith("(") && part.endsWith(")")) {
        bracketExpressions.add(part);
      }

      if (isKnownSpecifier(part)) {
        if (isStorageSpecifier(part)) {
          List<String> specifiers = new ArrayList<>();
          specifiers.add(part);
          finalType.setStorage(calcStorage(specifiers));
        } else if (isQualifierSpecifier(part)) {
          List<String> qualifiers = new ArrayList<>();
          qualifiers.add(part);
          finalType.setQualifier(calcQualifier(qualifiers, finalType.getQualifier()));
        }
      }
    }

    finalType = resolveBracketExpression(finalType, bracketExpressions);

    TypeManager typeManager = TypeManager.getInstance();
    finalType = typeManager.obtainType(finalType);

    return finalType;
  }
}
