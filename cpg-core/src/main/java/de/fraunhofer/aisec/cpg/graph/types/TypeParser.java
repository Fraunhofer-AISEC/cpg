/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.types;

import de.fraunhofer.aisec.cpg.frontends.*;
import de.fraunhofer.aisec.cpg.graph.LegacyTypeManager;
import de.fraunhofer.aisec.cpg.graph.Name;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for parsing the type definition and create the same Type as described by the
 * type string, but complying to the CPG TypeSystem
 */
public class TypeParser {

  private static final Logger log = LoggerFactory.getLogger(TypeParser.class);

  // TODO: document/remove this regexp
  private static final Pattern functionPtrRegex =
      Pattern.compile(
          "(?:(?<functionptr>[\\h(]+[a-zA-Z0-9_$.<>:]*\\*\\h*[a-zA-Z0-9_$.<>:]*[\\h)]+)\\h*)(?<args>\\(+[a-zA-Z0-9_$.<>,\\*\\&\\h]*\\))");
  private static final List<String> potentialKeywords =
      List.of(
          "STATIC",
          "EXTERN",
          "REGISTER",
          "AUTO",
          "FINAL",
          "CONST",
          "RESTRICT",
          "VOLATILE",
          "ATOMIC");

  private TypeParser() {
    throw new IllegalStateException("Do not instantiate the TypeParser");
  }

  /**
   * Returns whether the specifier is part of an elaborated type specifier. This only applies to C++
   * and can be used to declare that a type is a class / struct or union even though the type is not
   * visible in the scope.
   *
   * @param specifier the specifier
   * @return true, if it is part of an elaborated type. false, otherwise
   */
  public static boolean isElaboratedTypeSpecifier(
      String specifier, Language<? extends LanguageFrontend> language) {
    return language instanceof HasElaboratedTypeSpecifier hasElaboratedTypeSpecifier
        && hasElaboratedTypeSpecifier.getElaboratedTypeSpecifier().contains(specifier);
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
   * Matches the type blocks and checks if the typeString has the structure of a function pointer
   *
   * @param type separated type string
   * @return the Matcher of the functionPointer or null
   */
  @Nullable
  private static Matcher getFunctionPtrMatcher(@NotNull List<String> type) {

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

  private static boolean isUnknownType(
      String typeName, @NotNull Language<? extends LanguageFrontend> language) {
    return typeName.equals(Type.UNKNOWN_TYPE_STRING)
        || (language instanceof HasUnknownType hasUnknownType
            && hasUnknownType.getUnknownTypeString().contains(typeName));
  }

  /**
   * Removes spaces between a generics Expression, i.e. between "<" and ">" and the preceding Type
   * information Required since, afterwards typeString get split by spaces
   *
   * @param type typeString
   * @return typeString without spaces in the generic Expression
   */
  @NotNull
  private static String fixGenerics(
      @NotNull String type, @NotNull Language<? extends LanguageFrontend> language) {
    if (language instanceof HasGenerics hasGenerics
        && type.indexOf(hasGenerics.getStartCharacter()) > -1
        && type.indexOf(hasGenerics.getEndCharacter()) > -1) {

      char startCharacter = hasGenerics.getStartCharacter();
      char endCharacter = hasGenerics.getEndCharacter();

      // Get the generic string between startCharacter and endCharacter.
      String generics =
          type.substring(type.indexOf(startCharacter) + 1, type.lastIndexOf(endCharacter));
      if (language instanceof HasElaboratedTypeSpecifier hasElaboratedTypeSpecifier) {
        /* We can have elaborate type specifiers (e.g. struct) inside this string. We want to remove it.
         * We remove this specifier from the generic string.
         * To do so, this regex checks that a specifier is preceded by "<" (or whatever is the startCharacter), "," or a whitespace and is also followed by a whitespace (to avoid removing other strings by mistake).
         */
        generics =
            generics.replaceAll(
                "((^|[\\h,"
                    + hasGenerics.getStartCharacter()
                    + "])\\h*)(("
                    + String.join("|", hasElaboratedTypeSpecifier.getElaboratedTypeSpecifier())
                    + ")\\h+)",
                "$1");
      }
      // Add the generic to the original string again but also remove whitespaces in the generic.
      type =
          type.substring(0, type.indexOf(startCharacter) + 1)
              + generics.replaceAll("\\h", "").trim()
              + type.substring(type.lastIndexOf(endCharacter));
      // Remove unnecessary whitespace around the start and end characters.
      type = type.replaceAll("\\h*(" + startCharacter + "|" + endCharacter + "\\h?)\\h*", "$1");
    }

    return type;
  }

  private static void processBlockUntilLastSplit(
      @NotNull String type, int lastSplit, int newPosition, @NotNull List<String> typeBlocks) {
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
  @NotNull
  public static List<String> separate(
      @NotNull String type, Language<? extends LanguageFrontend> language) {
    type = type.split("=")[0];

    // Guarantee that there is no arbitrary number of whitespaces
    String[] typeSubpart = type.split(" ");
    type = String.join(" ", typeSubpart).trim();

    List<String> typeBlocks = new ArrayList<>();

    // Splits TypeString into relevant information blocks
    int lastSplit = 0;

    for (int i = 0; i < type.length(); i++) {
      char ch = type.charAt(i);
      switch (ch) {
        case ' ' -> {
          // handle space create element
          processBlockUntilLastSplit(type, lastSplit, i, typeBlocks);
          lastSplit = i + 1;
        }
        case '(' -> {
          // handle ( find matching closing ignore content (not relevant type information)
          processBlockUntilLastSplit(type, lastSplit, i, typeBlocks);
          int finishPosition = findMatching('(', ')', type.substring(i + 1));
          typeBlocks.add(type.substring(i, i + finishPosition + 1));
          i = finishPosition + i;
          lastSplit = i + 1;
        }
        case '[' -> {
          // handle [ find matching closing ignore content (not relevant type information)
          int finishPosition = findMatching('[', ']', type.substring(i + 1));
          Pattern onlyNumbers = Pattern.compile("^\\[[0-9]*\\]$");
          // If a language uses '[‘ for its generics, we want to make sure that only numbers (e.g.
          // for array sizes) are between the brackets. We assume that a type cannot be a number
          // here.
          if (!(language instanceof HasGenerics hasGenerics
                  && hasGenerics.getStartCharacter() == '[')
              || onlyNumbers.matcher(type.substring(i, i + finishPosition + 1)).matches()) {
            processBlockUntilLastSplit(type, lastSplit, i, typeBlocks);

            typeBlocks.add("[]"); // type.substring(i, i+finishPosition+1)
            i = finishPosition + i;
            lastSplit = i + 1;
          }
        }
        case '*' -> {
          // handle * operator
          processBlockUntilLastSplit(type, lastSplit, i, typeBlocks);
          typeBlocks.add("*");
          lastSplit = i + 1;
        }
        case '&' -> {
          // handle & operator
          processBlockUntilLastSplit(type, lastSplit, i, typeBlocks);
          typeBlocks.add("&");
          lastSplit = i + 1;
        }
        default -> {
          // everything else
          String substr = type.substring(lastSplit);
          if (substr.length() != 0 && i == type.length() - 1) {
            typeBlocks.add(substr);
          }
        }
      }
    }

    return typeBlocks;
  }

  private static List<Type> getParameterList(
      String parameterList, Language<? extends LanguageFrontend> language) {
    if (parameterList.startsWith("(") && parameterList.endsWith(")")) {
      parameterList = parameterList.trim().substring(1, parameterList.trim().length() - 1);
    }
    List<Type> parameters = new ArrayList<>();
    String[] parametersSplit = parameterList.split(",");
    for (String parameter : parametersSplit) {
      // ignore void parameters // TODO: WHY??
      if (parameter.length() > 0 && !parameter.trim().equals("void")) {
        parameters.add(createFrom(parameter.trim(), language));
      }
    }

    return parameters;
  }

  private static List<Type> getGenerics(
      String typeName, Language<? extends LanguageFrontend> language) {
    List<Type> genericList = new ArrayList<>();
    if (language instanceof HasGenerics hasGenerics
        && typeName.indexOf(hasGenerics.getStartCharacter()) > -1
        && typeName.indexOf(hasGenerics.getEndCharacter()) > -1) {
      String generics =
          typeName.substring(
              typeName.indexOf(hasGenerics.getStartCharacter()) + 1,
              typeName.lastIndexOf(hasGenerics.getEndCharacter()));

      String[] parametersSplit = generics.split(",");
      for (String parameter : parametersSplit) {
        genericList.add(createFrom(parameter.trim(), language));
      }
    }
    return genericList;
  }

  private static Type performBracketContentAction(
      Type finalType, String part, Language<? extends LanguageFrontend> language) {
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
      return resolveBracketExpression(finalType, List.of(part), language);
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
      @NotNull Type finalType,
      @NotNull List<String> bracketExpressions,
      @NotNull Language<? extends LanguageFrontend> language) {
    for (String bracketExpression : bracketExpressions) {
      List<String> splitExpression =
          separate(bracketExpression.substring(1, bracketExpression.length() - 1), language);
      for (String part : splitExpression) {
        finalType = performBracketContentAction(finalType, part, language);
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
  private static String removeAccessModifier(
      @NotNull String type, @NotNull Language<? extends LanguageFrontend> language) {
    return type.replaceAll(String.join("|", language.getAccessModifiers()), "").trim();
  }

  /**
   * Checks if the List of separated parts of the typeString contains an element indicating that it
   * is a primitive type
   *
   * @param stringList
   * @return
   */
  private static boolean isPrimitiveType(
      @NotNull List<String> stringList, @NotNull Language<? extends LanguageFrontend> language) {
    return stringList.stream().anyMatch(s -> language.getPrimitiveTypes().contains(s));
  }

  /**
   * Joins compound primitive data types such as long long int and consolidates those information
   * blocks
   *
   * @param typeBlocks
   * @return separated words of compound types are joined into one string
   */
  @NotNull
  private static List<String> joinPrimitive(
      @NotNull List<String> typeBlocks, @NotNull Language<? extends LanguageFrontend> language) {
    List<String> joinedTypeBlocks = new ArrayList<>();
    StringBuilder primitiveType = new StringBuilder();
    int index = 0;

    for (String s : typeBlocks) {
      if (language.getPrimitiveTypes().contains(s)) {
        if (primitiveType.length() > 0) {
          primitiveType.append(" ");
        } else {
          index = joinedTypeBlocks.size();
        }
        primitiveType.append(s);
      } else {
        joinedTypeBlocks.add(s);
      }
    }

    if (!primitiveType.isEmpty()) {
      joinedTypeBlocks.add(index, primitiveType.toString());
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
  @NotNull
  public static Type reWrapType(@NotNull Type oldChain, @NotNull Type newRoot) {
    if (oldChain.isFirstOrderType()) {
      newRoot.setTypeOrigin(oldChain.getTypeOrigin());
    }

    if (!newRoot.isFirstOrderType()) {
      return newRoot;
    }

    if (oldChain instanceof ObjectType && newRoot instanceof ObjectType) {
      ((ObjectType) newRoot.getRoot()).setGenerics(((ObjectType) oldChain).getGenerics());
      return newRoot;
    } else if (oldChain instanceof ReferenceType referenceType) {
      Type reference = reWrapType(referenceType.getElementType(), newRoot);
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
   * Does the same as {@link #createIgnoringAlias(String, Language)} but explicitly does not use
   * type alias resolution. This is usually not what you want. Use with care!
   *
   * @param string the string representation of the type
   * @return the type
   */
  @NotNull
  public static Type createIgnoringAlias(
      @NotNull String string, @NotNull Language<? extends LanguageFrontend> language) {
    return createFrom(string, language);
  }

  @NotNull
  private static Type postTypeParsing(
      @NotNull List<String> subPart,
      @NotNull Type finalType,
      @NotNull List<String> bracketExpressions) {
    for (String part : subPart) {
      if (part.equals("*")) {
        // Creates a Pointer to the finalType
        finalType = finalType.reference(PointerType.PointerOrigin.POINTER);
      }

      if (part.equals("&")) {
        // CPP ReferenceTypes are indicated by an & at the end of the typeName e.g. int&, and are
        // handled differently to a pointer
        finalType = new ReferenceType(finalType);
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
    }
    return finalType;
  }

  private static String removeGenerics(
      String typeName, @NotNull Language<? extends LanguageFrontend> language) {
    if (language instanceof HasGenerics hasGenerics
        && typeName.contains(hasGenerics.getStartCharacter() + "")
        && typeName.contains(hasGenerics.getEndCharacter() + "")) {
      typeName = typeName.substring(0, typeName.indexOf(hasGenerics.getStartCharacter()));
    }
    return typeName;
  }

  private static String determineModifier(List<String> typeBlocks, boolean primitiveType) {
    // Default is signed, unless unsigned keyword is specified. For other classes that are not
    // primitive this is NOT_APPLICABLE
    String modifier = "";
    if (primitiveType) {
      if (typeBlocks.contains("unsigned")) {
        modifier = "unsigned ";
        typeBlocks.remove("unsigned");
      } else if (typeBlocks.contains("signed")) {
        modifier = "signed ";
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
  @NotNull
  private static Type createFromUnsafe(
      @NotNull String type,
      boolean resolveAlias,
      @NotNull Language<? extends LanguageFrontend> language,
      @Nullable ScopeManager scopeManager) {
    // Check if Problems during Parsing
    if (!checkValidTypeString(type)) {
      return UnknownType.getUnknownType(language);
    }

    // Preprocessing of the typeString
    type = removeAccessModifier(type, language);

    // Determine if inner class

    type = fixGenerics(type, language);

    // Separate typeString into a List containing each part of the typeString
    List<String> typeBlocks = separate(type, language);

    // Depending on if the Type is primitive or not signed/unsigned must be set differently (only
    // relevant for ObjectTypes)
    boolean primitiveType = isPrimitiveType(typeBlocks, language);

    // Default is signed, unless unsigned keyword is specified. For other classes that are not
    // primitive this is NOT_APPLICABLE
    String modifier = determineModifier(typeBlocks, primitiveType);

    // Join compound primitive types into one block i.e. types consisting of more than one word e.g.
    // long long int (only primitive types)
    typeBlocks = joinPrimitive(typeBlocks, language);

    // Handle preceding qualifier or storage specifier to the type name e.g. static const int
    int counter = 0;

    for (String part : typeBlocks) {
      if (potentialKeywords.contains(part.toUpperCase())
          || isElaboratedTypeSpecifier(part, language)) {
        // We only want to get rid of these parts for the remaining method.
        counter++;
      } else {
        break;
      }
    }

    // Once all preceding known keywords (if any) are handled the next word must be the TypeName
    if (counter >= typeBlocks.size()) {
      // Note that "const auto ..." will end here with typeName="const" as auto is not supported.
      return UnknownType.getUnknownType(language);
    }
    String typeName = typeBlocks.get(counter);
    counter++;

    Type finalType;
    LegacyTypeManager typeManager = LegacyTypeManager.getInstance();

    // Check if type is FunctionPointer
    Matcher funcptr = getFunctionPtrMatcher(typeBlocks.subList(counter, typeBlocks.size()));

    finalType = language.getSimpleTypeOf(modifier + typeName);
    if (finalType != null) {
      // Nothing to do here
    } else if (funcptr != null) {
      Type returnType = createFrom(typeName, language);
      List<Type> parameterList = getParameterList(funcptr.group("args"), language);

      return typeManager.registerType(new FunctionPointerType(parameterList, returnType, language));
    } else if (isIncompleteType(typeName)) {
      // IncompleteType e.g. void
      finalType = new IncompleteType();
    } else if (isUnknownType(typeName, language)) {
      // UnknownType -> no information on how to process this type
      finalType = new UnknownType(Type.UNKNOWN_TYPE_STRING);
    } else {
      // ObjectType
      // Obtain possible generic List from TypeString
      List<Type> generics = getGenerics(typeName, language);
      typeName = removeGenerics(typeName, language);
      finalType =
          new ObjectType(
              typeName, generics, ObjectType.Modifier.NOT_APPLICABLE, primitiveType, language);
    }

    // Process Keywords / Operators (*, &) after typeName
    List<String> subPart = typeBlocks.subList(counter, typeBlocks.size());

    List<String> bracketExpressions = new ArrayList<>();

    finalType = postTypeParsing(subPart, finalType, bracketExpressions);

    // Resolve BracketExpressions that were identified previously
    finalType = resolveBracketExpression(finalType, bracketExpressions, language);

    // Make sure, that only one real instance exists for a type in order to have just one node in
    // the graph representing the type
    finalType = typeManager.registerType(finalType);

    if (resolveAlias && scopeManager != null) {
      return typeManager.registerType(typeManager.resolvePossibleTypedef(finalType, scopeManager));
    }

    return finalType;
  }

  /**
   * A specialized version of the type parsing function that needs a language frontend and does
   * magic with generics and typedefs. This is legacy code and currently only used for CXX frontend
   * and should be removed at some point.
   */
  public static Type createFrom(@NotNull String type, boolean resolveAlias, LanguageFrontend lang) {
    Type templateType = searchForTemplateTypes(type, lang.getScopeManager());
    if (templateType != null) {
      return templateType;
    }

    Type createdType = createFrom(type, lang.getLanguage(), resolveAlias, lang.getScopeManager());

    if (createdType instanceof SecondOrderType) {
      templateType =
          searchForTemplateTypes(
              createdType.getRoot().getName().toString(), lang.getScopeManager());
      if (templateType != null) {
        createdType.setRoot(templateType);
      }
    }

    return createdType;
  }

  private static Type searchForTemplateTypes(@NotNull String type, ScopeManager scopeManager) {
    return LegacyTypeManager.getInstance()
        .searchTemplateScopeForDefinedParameterizedTypes(scopeManager.getCurrentScope(), type);
  }

  /**
   * Use this function for parsing new types and obtaining a new Type the TypeParser creates from
   * the typeString.
   *
   * @param type string with type information
   * @param language the language in which the type exists.
   * @param resolveAlias should replace with original type in typedefs
   * @param scopeManager optional, but required if resolveAlias is true
   * @return new type representing the type string. If an exception occurs during the parsing,
   *     UnknownType is returned
   */
  @NotNull
  public static Type createFrom(
      @NotNull String type,
      Language<? extends LanguageFrontend> language,
      boolean resolveAlias,
      ScopeManager scopeManager) {
    try {
      return createFromUnsafe(type, resolveAlias, language, scopeManager);
    } catch (Exception e) {
      log.error("Could not parse the type correctly", e);
      return UnknownType.getUnknownType(language);
    }
  }

  /** Parses the type from a string and the supplied language. */
  @NotNull
  public static Type createFrom(
      @NotNull String type, Language<? extends LanguageFrontend> language) {
    return createFrom(type, language, false, null);
  }

  /** Parses the type from a string and the supplied language. */
  @NotNull
  public static Type createFrom(@NotNull Name name, Language<? extends LanguageFrontend> language) {
    return createFrom(name.toString(), language, false, null);
  }
}
