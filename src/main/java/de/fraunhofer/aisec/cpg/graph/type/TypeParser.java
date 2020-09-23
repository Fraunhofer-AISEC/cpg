/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

package de.fraunhofer.aisec.cpg.graph.type;

import de.fraunhofer.aisec.cpg.graph.TypeManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for parsing the type definition and create the same Type as described by the
 * type string, but complying to the CPG TypeSystem
 */
public class TypeParser {

  private static final Logger log = LoggerFactory.getLogger(TypeParser.class);

  public static final String UNKNOWN_TYPE_STRING = "UNKNOWN";
  private static final List<String> primitives =
      List.of("byte", "short", "int", "long", "float", "double", "boolean", "char");
  private static final Pattern functionPtrRegex =
      Pattern.compile(
          "(?:(?<functionptr>(\\h|\\()+[a-zA-Z0-9_$.<>:]*\\*\\h*[a-zA-Z0-9_$.<>:]*(\\h|\\))+)\\h*)(?<args>\\(+[a-zA-Z0-9_$.<>,\\*\\&\\h]*\\))");

  private static Supplier<TypeManager.Language> languageSupplier =
      () -> TypeManager.getInstance().getLanguage();
  private static final String VOLATILE_QUALIFIER = "volatile";
  private static final String FINAL_QUALIFIER = "final";
  private static final String CONST_QUALIFIER = "const";
  private static final String RESTRICT_QUALIFIER = "restrict";
  private static final String ATOMIC_QUALIFIER = "atomic";

  private static final String ELABORATED_TYPE_CLASS = "class";
  private static final String ELABORATED_TYPE_STRUCT = "struct";
  private static final String ELABORATED_TYPE_UNION = "union";
  private static final String ELABORATED_TYPE_ENUM = "enum";

  private static final List<String> elaboratedTypes =
      List.of(
          ELABORATED_TYPE_CLASS,
          ELABORATED_TYPE_STRUCT,
          ELABORATED_TYPE_UNION,
          ELABORATED_TYPE_ENUM);

  private TypeParser() {
    throw new IllegalStateException("Do not instantiate the TypeParser");
  }

  public static void reset() {
    TypeParser.languageSupplier = () -> TypeManager.getInstance().getLanguage();
  }

  /**
   * WARNING: This is only intended for Test Purposes of the TypeParser itself without parsing
   * files. Do not use this.
   *
   * @param languageSupplier frontend language Java or CPP
   */
  public static void setLanguageSupplier(Supplier<TypeManager.Language> languageSupplier) {
    TypeParser.languageSupplier = languageSupplier;
  }

  public static TypeManager.Language getLanguage() {
    return TypeParser.languageSupplier.get();
  }

  /**
   * Infers corresponding qualifier information for the type depending of the keywords.
   *
   * @param typeString list of found keywords
   * @param old previous qualifier information which is completed with newer qualifier information
   * @return Type.Qualifier
   */
  public static Type.Qualifier calcQualifier(List<String> typeString, Type.Qualifier old) {
    boolean constantFlag = false;
    boolean volatileFlag = false;
    boolean restrictFlag = false;
    boolean atomicFlag = false;

    if (old != null) {
      constantFlag = old.isConst();
      volatileFlag = old.isVolatile();
      restrictFlag = old.isRestrict();
      atomicFlag = old.isAtomic();
    }

    for (String part : typeString) {
      switch (part) {
        case FINAL_QUALIFIER:
        case CONST_QUALIFIER:
          constantFlag = true;
          break;

        case VOLATILE_QUALIFIER:
          volatileFlag = true;
          break;

        case RESTRICT_QUALIFIER:
          restrictFlag = true;
          break;

        case ATOMIC_QUALIFIER:
          atomicFlag = true;
          break;
        default:
          // do nothing
      }
    }

    return new Type.Qualifier(constantFlag, volatileFlag, restrictFlag, atomicFlag);
  }

  /**
   * Infers the corresponding storage type depending on the present storage keyword. Default AUTO
   *
   * @param typeString List of storage keywords
   * @return Storage
   */
  public static Type.Storage calcStorage(List<String> typeString) {
    for (String part : typeString) {
      try {
        return Type.Storage.valueOf(part.toUpperCase());
      } catch (IllegalArgumentException ignored) {
        // continue in case of illegalArgumentException
      }
    }
    return Type.Storage.AUTO;
  }

  public static boolean isStorageSpecifier(String specifier) {
    if (getLanguage() == TypeManager.Language.CXX) {
      return specifier.equalsIgnoreCase("STATIC");
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
    if (getLanguage() == TypeManager.Language.JAVA) {
      return qualifier.equals(FINAL_QUALIFIER) || qualifier.equals(VOLATILE_QUALIFIER);
    } else {
      return qualifier.equals(CONST_QUALIFIER)
          || qualifier.equals(VOLATILE_QUALIFIER)
          || qualifier.equals(RESTRICT_QUALIFIER)
          || qualifier.equals(ATOMIC_QUALIFIER);
    }
  }

  /**
   * Returns whether the specifier is part of an elaborated type specifier. This only applies to C++
   * and can be used to declare that a type is a class / struct or union even though the type is not
   * visible in the scope.
   *
   * @param specifier the specifier
   * @return true, if it is part of an elaborated type. false, otherwise
   */
  public static boolean isElaboratedTypeSpecifier(String specifier) {
    if (getLanguage() == TypeManager.Language.CXX) {
      return specifier.equals(ELABORATED_TYPE_CLASS)
          || specifier.equals(ELABORATED_TYPE_STRUCT)
          || specifier.equals(ELABORATED_TYPE_UNION)
          || specifier.equals(ELABORATED_TYPE_ENUM);
    }

    return false;
  }

  public static boolean isKnownSpecifier(String specifier) {
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
      if (i >= string.length()) {
        // dirty hack for now
        return string.length();
      }

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

  /**
   * Matches the type blocks and checks if it the typeString has the structure of a function pointer
   *
   * @param type separated type string
   * @return true if function pointer structure is found in typeString, false if not
   */
  @Nullable
  private static Matcher getFunctionPtrMatcher(@NonNull List<String> type) {

    StringBuilder typeStringBuilder = new StringBuilder();
    for (String typePart : type) {
      typeStringBuilder.append(typePart);
    }

    String typeString = typeStringBuilder.toString().trim();

    Matcher matcher = functionPtrRegex.matcher(typeString);
    if (matcher.find()) {
      return matcher;
    }
    return null;
  }

  /**
   * Right now IncompleteTypes are only defined as void {@link IncompleteType}
   *
   * @param typeName String with the type
   * @return true if the type is void, false otherwise
   */
  private static boolean isIncompleteType(String typeName) {
    return typeName.trim().equals("void");
  }

  private static boolean isUnknownType(String typeName) {
    return typeName.equals(UNKNOWN_TYPE_STRING);
  }

  /**
   * Removes spaces between a generics Expression, i.e. between "<" and ">" and the preceding Type
   * information Required since, afterwards typeString get splitted by spaces
   *
   * @param type typeString
   * @return typeString without spaces in the generic Expression
   */
  @NonNull
  private static String fixGenerics(@NonNull String type) {
    if (type.contains("<") && type.contains(">") && getLanguage() == TypeManager.Language.CXX) {
      String generics = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>'));

      /* Explanation from @vfsrfs:
       * We fist extract the generic string (the substring between < and >). Then, the elaborate
       * string can either start directly with the elaborate type specifier e.g. struct Node or it
       * must be preceded by <, \\h (horizontal whitespace), or ,. If any other character precedes
       * the elaborate type specifier then it is not considered to be a type specifier e.g.
       * mystruct. Then there can be an arbitrary amount of horizontal whitespaces. This is followed
       * by the elaborate type specifier and at least one more horizontal whitespace, which marks
       * that it is indeed an elaborate type and not something like structMy.
       */
      for (String elaborate : elaboratedTypes) {
        generics = generics.replaceAll("(^|(?<=[\\h,<]))\\h*(?<main>" + elaborate + "\\h+)", "");
      }
      type =
          type.substring(0, type.indexOf('<') + 1)
              + generics.trim()
              + type.substring(type.lastIndexOf('>'));
    }

    StringBuilder out = new StringBuilder();
    int bracketCount = 0;
    int iterator = 0;
    while (iterator < type.length()) {
      switch (type.charAt(iterator)) {
        case '<':
          bracketCount++;
          out.append(type.charAt(iterator));
          break;

        case '>':
          out.append('>');
          bracketCount--;
          break;

        case ' ':
          if (bracketCount == 0) {
            out.append(type.charAt(iterator));
          }
          break;

        default:
          out.append(type.charAt(iterator));
          break;
      }
      iterator++;
    }

    String[] splitted = out.toString().split("\\<");
    StringBuilder out2 = new StringBuilder();
    for (String s : splitted) {
      if (out2.length() > 0) {
        out2.append('<');
      }
      out2.append(s.trim());
    }

    return out2.toString();
  }

  private static void processBlockUntilLastSplit(
      @NonNull String type, int lastSplit, int newPosition, @NonNull List<String> typeBlocks) {
    String substr = type.substring(lastSplit, newPosition);
    if (substr.length() != 0) {
      typeBlocks.add(substr);
    }
  }

  /**
   * Separates typeString into the different Parts that make up the type information
   *
   * @param type string with the entire type definition
   * @return list of strings in which every piece of type information is one element of the list
   */
  @NonNull
  public static List<String> separate(@NonNull String type) {

    // Remove :: CPP operator, use . instead
    type = type.replace("::", ".");
    type = type.split("=")[0];

    // Guarantee that there is no arbitrary number of whitespaces
    String[] typeSubpart = type.split(" ");
    type = String.join(" ", typeSubpart).trim();

    List<String> typeBlocks = new ArrayList<>();

    // Splits TypeString into relevant information blocks
    int lastSplit = 0;
    int finishPosition = 0;
    String substr = "";

    int i = 0;
    while (i < type.length()) {
      char ch = type.charAt(i);
      switch (ch) {
        case ' ':
          // handle space create element
          processBlockUntilLastSplit(type, lastSplit, i, typeBlocks);
          lastSplit = i + 1;
          break;

        case '(':
          // handle ( find matching closing ignore content (not relevant type information)
          processBlockUntilLastSplit(type, lastSplit, i, typeBlocks);
          finishPosition = findMatching('(', ')', type.substring(i + 1));
          typeBlocks.add(type.substring(i, i + finishPosition + 1));
          i = finishPosition + i;
          lastSplit = i + 1;
          break;

        case '[':
          // handle [ find matching closing ignore content (not relevant type information)
          processBlockUntilLastSplit(type, lastSplit, i, typeBlocks);

          finishPosition = findMatching('[', ']', type.substring(i + 1));
          typeBlocks.add("[]"); // type.substring(i, i+finishPosition+1)
          i = finishPosition + i;
          lastSplit = i + 1;
          break;

        case '*':
          // handle * operator
          processBlockUntilLastSplit(type, lastSplit, i, typeBlocks);

          typeBlocks.add("*");
          lastSplit = i + 1;
          break;

        case '&':
          // handle & operator
          processBlockUntilLastSplit(type, lastSplit, i, typeBlocks);

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
      i++;
    }

    return typeBlocks;
  }

  private static List<Type> getParameterList(String parameterList) {
    if (parameterList.startsWith("(") && parameterList.endsWith(")")) {
      parameterList = parameterList.trim().substring(1, parameterList.trim().length() - 1);
    }
    List<Type> parameters = new ArrayList<>();
    String[] parametersSplit = parameterList.split(",");
    for (String parameter : parametersSplit) {
      // ignore void parameters
      if (parameter.length() > 0 && !parameter.trim().equals("void")) {
        parameters.add(createFrom(parameter.trim(), true));
      }
    }

    return parameters;
  }

  private static List<Type> getGenerics(String typeName) {
    if (typeName.contains("<") && typeName.contains(">")) {
      String generics = typeName.substring(typeName.indexOf('<') + 1, typeName.lastIndexOf('>'));
      List<Type> genericList = new ArrayList<>();
      String[] parametersSplit = generics.split(",");
      for (String parameter : parametersSplit) {
        genericList.add(createFrom(parameter.trim(), true));
      }

      return genericList;
    }
    return new ArrayList<>();
  }

  private static Type performBracketContentAction(Type finalType, String part) {
    if (part.equals("*")) {
      return finalType.reference(PointerType.PointerOrigin.POINTER);
    }

    if (part.equals("&")) {
      return finalType.dereference();
    }

    if (part.startsWith("[") && part.endsWith("]")) {
      return finalType.reference(PointerType.PointerOrigin.ARRAY);
    }
    if (part.startsWith("(") && part.endsWith(")")) {
      List<String> subBracketExpression = new ArrayList<>();
      subBracketExpression.add(part);
      return resolveBracketExpression(finalType, subBracketExpression);
    }

    if (isStorageSpecifier(part)) {
      List<String> specifiers = new ArrayList<>();
      specifiers.add(part);
      finalType.setStorage(calcStorage(specifiers));
      return finalType;
    }

    if (isQualifierSpecifier(part)) {
      List<String> qualifiers = new ArrayList<>();
      qualifiers.add(part);
      finalType.setQualifier(calcQualifier(qualifiers, finalType.getQualifier()));
      return finalType;
    }

    return finalType;
  }

  /**
   * Makes sure to apply Expressions containing brackets that change the binding of operators e.g.
   * () can change the binding order of operators
   *
   * @param finalType Modifications are applied to this type which is the result of the preceding
   *     type calculations
   * @param bracketExpressions List of Strings containing bracket expressions
   * @return modified finalType performing the resolution of the bracket expressions
   */
  private static Type resolveBracketExpression(
      @NonNull Type finalType, @NonNull List<String> bracketExpressions) {
    for (String bracketExpression : bracketExpressions) {
      List<String> splitExpression =
          separate(bracketExpression.substring(1, bracketExpression.length() - 1));
      for (String part : splitExpression) {
        finalType = performBracketContentAction(finalType, part);
      }
    }

    return finalType;
  }

  /**
   * Helper function that removes access modifier from the typeString.
   *
   * @param type provided typeString
   * @return typeString without access modifier
   */
  private static String removeAccessModifier(@NonNull String type) {
    return type.replaceAll("public|private|protected", "").trim();
  }

  /**
   * Replaces the Scope Resolution Operator (::) in C++ by . for a consistent parsing
   *
   * @param type provided typeString
   * @return typeString which uses . instead of the substring :: if CPP is the current language
   */
  private static String replaceScopeResolutionOperator(@NonNull String type) {
    return (getLanguage() == TypeManager.Language.CXX) ? type.replace("::", ".").trim() : type;
  }

  /**
   * Checks if the List of separated parts of the typeString contains an element indicating that it
   * is a primitive type
   *
   * @param stringList
   * @return
   */
  private static boolean isPrimitiveType(@NonNull List<String> stringList) {
    for (String s : stringList) {
      if (primitives.contains(s)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Joins compound primitive data types such as long long int and consolidates those information
   * blocks
   *
   * @param typeBlocks
   * @return separated words of compound types are joined into one string
   */
  @NonNull
  private static List<String> joinPrimitive(@NonNull List<String> typeBlocks) {
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

  /**
   * Reconstructs the type chain when the root node is modified e.g. when swapping with alias
   * (typedef)
   *
   * @param oldChain containing all types until the root
   * @param newRoot root the chain is swapped with
   * @return oldchain but root replaced with newRoot
   */
  @NonNull
  public static Type reWrapType(@NonNull Type oldChain, @NonNull Type newRoot) {
    if (oldChain.isFirstOrderType()) {
      newRoot.setTypeOrigin(oldChain.getTypeOrigin());
    }

    if (!newRoot.isFirstOrderType()) {
      return newRoot;
    }

    if (oldChain instanceof ObjectType && newRoot instanceof ObjectType) {
      ((ObjectType) newRoot.getRoot()).setGenerics(((ObjectType) oldChain).getGenerics());
      return newRoot;
    } else if (oldChain instanceof ReferenceType) {
      Type reference = reWrapType(((ReferenceType) oldChain).getElementType(), newRoot);
      ReferenceType newChain = (ReferenceType) oldChain.duplicate();
      newChain.setElementType(reference);
      newChain.refreshName();
      return newChain;
    } else if (oldChain instanceof PointerType) {
      PointerType newChain = (PointerType) oldChain.duplicate();
      newChain.setRoot(reWrapType(oldChain.getRoot(), newRoot));
      newChain.refreshNames();
      return newChain;
    } else {
      return newRoot;
    }
  }

  /**
   * Does the same as {@link #createIgnoringAlias(String)} but explicitly does not use type alias
   * resolution. This is usually not what you want. Use with care!
   *
   * @param string the string representation of the type
   * @return the type
   */
  @NonNull
  public static Type createIgnoringAlias(@NonNull String string) {
    return createFrom(string, false);
  }

  @NonNull
  private static Type postTypeParsing(
      @NonNull List<String> subPart,
      @NonNull Type finalType,
      @NonNull List<String> bracketExpressions) {
    for (String part : subPart) {
      if (part.equals("*")) {
        // Creates a Pointer to the finalType
        finalType = finalType.reference(PointerType.PointerOrigin.POINTER);
      }

      if (part.equals("&")) {
        // CPP ReferenceTypes are indicated by an & at the end of the typeName e.g. int&, and are
        // handled differently to a pointer
        Type.Qualifier oldQualifier = finalType.getQualifier();
        Type.Storage oldStorage = finalType.getStorage();
        finalType.setQualifier(new Type.Qualifier());
        finalType.setStorage(Type.Storage.AUTO);
        finalType = new ReferenceType(finalType);
        finalType.setStorage(oldStorage);
        finalType.setQualifier(oldQualifier);
      }

      if (part.startsWith("[") && part.endsWith("]")) {
        // Arrays are equal to pointer, create a reference
        finalType = finalType.reference(PointerType.PointerOrigin.ARRAY);
      }

      if (part.startsWith("(") && part.endsWith(")")) {
        // BracketExpressions change the binding of operators they are stored in order to be
        // processed afterwards
        bracketExpressions.add(part);
      }

      // Check storage and qualifiers specifierd that are defined after the typeName e.g. int const
      if (isStorageSpecifier(part)) {
        List<String> specifiers = new ArrayList<>();
        specifiers.add(part);
        finalType.setStorage(calcStorage(specifiers));
      }

      if (isQualifierSpecifier(part)) {
        List<String> qualifiers = new ArrayList<>();
        qualifiers.add(part);
        finalType.setQualifier(calcQualifier(qualifiers, finalType.getQualifier()));
      }
    }
    return finalType;
  }

  private static String removeGenerics(String typeName) {
    if (typeName.contains("<") && typeName.contains(">")) {
      typeName = typeName.substring(0, typeName.indexOf('<'));
    }
    return typeName;
  }

  private static ObjectType.Modifier determineModifier(
      List<String> typeBlocks, boolean primitiveType) {
    // Default is signed, unless unsigned keyword is specified. For other classes that are not
    // primitive this is NOT_APPLICABLE
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
    return modifier;
  }

  private static boolean checkValidTypeString(String type) {
    // Todo ? can be part of generic string -> more fine-grained analysis necessary
    return !type.contains("?")
        && !type.contains("org.eclipse.cdt.internal.core.dom.parser.ProblemType@")
        && type.trim().length() != 0;
  }

  /**
   * Warning: This function might crash, when a type cannot be parsed. Use createFrom instead Use
   * this function for parsing new types and obtaining a new Type the TypeParser creates from the
   * typeString
   *
   * @param type string with type information
   * @param resolveAlias should replace with original type in typedefs
   * @return new type representing the type string
   */
  @NonNull
  private static Type createFromUnsafe(@NonNull String type, boolean resolveAlias) {
    // Check if Problems during Parsing
    if (!checkValidTypeString(type)) {
      return UnknownType.getUnknownType();
    }

    // Preprocessing of the typeString
    type = removeAccessModifier(type);
    // Remove CPP :: Operator
    type = replaceScopeResolutionOperator(type);

    // Determine if inner class

    type = fixGenerics(type);

    // Separate typeString into a List containing each part of the typeString
    List<String> typeBlocks = separate(type);

    // Depending if the Type is primitive or not signed/unsigned must be set differently (only
    // relevant for ObjectTypes)
    boolean primitiveType = isPrimitiveType(typeBlocks);

    // Default is signed, unless unsigned keyword is specified. For other classes that are not
    // primitive this is NOT_APPLICABLE
    ObjectType.Modifier modifier = determineModifier(typeBlocks, primitiveType);

    // Join compound primitive types into one block i.e. types consisting of more than one word e.g.
    // long long int (only primitive types)
    typeBlocks = joinPrimitive(typeBlocks);

    List<String> qualifierList = new ArrayList<>();
    List<String> storageList = new ArrayList<>();

    // Handle preceding qualifier or storage specifier to the type name e.g. static const int
    int counter = 0;
    for (String part : typeBlocks) {
      if (isQualifierSpecifier(part)) {
        qualifierList.add(part);
        counter++;
      } else if (isStorageSpecifier(part)) {
        storageList.add(part);
        counter++;
      } else if (isElaboratedTypeSpecifier(part)) {
        // ignore elaborated types for now
        counter++;
      } else {
        break;
      }
    }

    Type.Storage storageValue = calcStorage(storageList);
    Type.Qualifier qualifier = calcQualifier(qualifierList, null);

    // Once all preceding known keywords (if any) are handled the next word must be the TypeName
    if (counter >= typeBlocks.size()) {
      // Note that "const auto ..." will end here with typeName="const" as auto is not supported.
      return UnknownType.getUnknownType();
    }
    String typeName = typeBlocks.get(counter);
    counter++;

    Type finalType;
    TypeManager typeManager = TypeManager.getInstance();

    // Check if type is FunctionPointer
    Matcher funcptr = getFunctionPtrMatcher(typeBlocks.subList(counter, typeBlocks.size()));

    if (funcptr != null) {
      Type returnType = createFrom(typeName, false);
      List<Type> parameterList = getParameterList(funcptr.group("args"));

      return typeManager.registerType(
          new FunctionPointerType(qualifier, storageValue, parameterList, returnType));
    } else if (isIncompleteType(typeName)) {
      // IncompleteType e.g. void
      finalType = new IncompleteType();
    } else if (isUnknownType(typeName)) {
      // UnknownType -> no information on how to process this type
      finalType = new UnknownType(typeName);
    } else {
      // ObjectType
      // Obtain possible generic List from TypeString
      List<Type> generics = getGenerics(typeName);
      typeName = removeGenerics(typeName);
      finalType =
          new ObjectType(typeName, storageValue, qualifier, generics, modifier, primitiveType);
    }

    if (finalType.getTypeName().equals("auto") || (type.contains("auto") && !primitiveType)) {
      // In C++17 if auto keyword is used the compiler infers the type automatically, hence we
      // are not able to find out, which type this should be, it will be resolved due to
      // dataflow
      return UnknownType.getUnknownType();
    }

    // Process Keywords / Operators (*, &) after typeName
    List<String> subPart = typeBlocks.subList(counter, typeBlocks.size());

    List<String> bracketExpressions = new ArrayList<>();

    finalType = postTypeParsing(subPart, finalType, bracketExpressions);

    // Resolve BracketExpressions that were identified previously
    finalType = resolveBracketExpression(finalType, bracketExpressions);

    // Make sure, that only one real instance exists for a type in order to have just one node in
    // the graph representing the type
    finalType = typeManager.registerType(finalType);

    if (resolveAlias) {
      return typeManager.registerType(typeManager.resolvePossibleTypedef(finalType));
    }

    return finalType;
  }

  /**
   * Use this function for parsing new types and obtaining a new Type the TypeParser creates from *
   * the typeString
   *
   * @param type string with type information
   * @param resolveAlias should replace with original type in typedefs
   * @return new type representing the type string. If an exception occurs during the parsing,
   *     UnknownType is returned
   */
  @NonNull
  public static Type createFrom(@NonNull String type, boolean resolveAlias) {
    try {
      return createFromUnsafe(type, resolveAlias);
    } catch (Exception e) {
      log.error("Could not parse the type correctly", e);
      return UnknownType.getUnknownType();
    }
  }
}
