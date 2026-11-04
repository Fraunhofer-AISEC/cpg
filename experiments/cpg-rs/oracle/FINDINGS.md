# Kotlin C frontend: findings from the Rust port (candidates to fix later in Kotlin)

Source: differential comparison of the Rust port (`cparse/`) against the Kotlin CPG dump
(`TmpDump.kt`, frontend + ImportResolver/TypeResolver/TypeHierarchyResolver/EOG/SymbolResolver)
on the 27 top-level C files in `cpg-language-cxx/src/test/resources/c`.
Policy: the Rust port replicates every quirk first (parity), and each one is listed here. Nothing
below has been changed in Kotlin. "Verdict" is a first guess, not a decision.

| # | Where | Observation | Example | Verdict |
|---|---|---|---|---|
| 1 | Enumeration location | `typedef enum {...} E;` ranges over the whole typedef incl. the `;`; a `typedef struct {...} S;` Record ends at `}`. Inconsistent between the two. | enum.c: Enumeration 1:1-5:10 vs typedef_struct.c: Record 1:1-4:2 | probably bug / inconsistency |
| 2 | TranslationUnit location | With a trailing newline the end column is last char + 2 (newline counted as a column of its line). | goto.c ends `}\n`: TU 1:1-5:3 | probably bug (off by one) |
| 3 | Typedef | No location and no code. | `Typedef 'S' loc=?` | check, likely unintended |
| 4 | Variable location | Declarator range includes the pointer operator. | `S* p=&s;` -> Variable `p` code `* p=&s`, 9:4-9:10 | check; maybe intended (CDT declarator) |
| 5 | EnumConstant name | Constant of an unnamed enum is named `::THIS`. | enum.c | probably bug (leading `::`) |
| 6 | Record in C | Implicit `Constructor` with a `this` Variable is created for C structs. | typedef_struct.c: `Constructor 'test::test'` | check, C has no constructors |
| 7 | Expression statements | The expression (e.g. Call/Assign) takes the statement range including the `;`; the inner Reference/Literal do not. | `s.a=1;` Assign 10:3-10:9 | by design? inconsistent with other nodes |
| 8 | Block / Assign / Call / Return type | `UNKNOWN` type for statements and calls without a type. | `Block type='UNKNOWN'` | probably fine, note |
| 9 | Struct fields | `int x, y, z;` (several declarators in one field declaration) creates NO Field; they only show up later as inferred fields (loc `?`) when used, e.g. by designated initializers. A single declarator (`struct Point p;`) works. | designated.c: Record `Point` has Fields `y`,`z`,`x` with loc `?`, none at 3:9 | **likely bug** |
| 10 | Cast operand | A parenthesized operand of a cast becomes a `UnaryOperator '()'`, while parentheses are transparent everywhere else. | `(size_t)(p)` -> Cast > UnaryOperator `()` > Reference | check, inconsistent |
| 11 | Cast name | Cast is named after the *resolved* target type (typedef followed), not the written one. | `(size_t)(p)` with `typedef int size_t` -> Cast `int` | probably fine; needs type layer |
| 12 | ArrayConstruction | Range is the whole declaration statement (`char buf[len + 1];`), not the array declarator; only created when there is no initializer. | arrays.c | check |
| 13 | Prototype range | A function prototype's range is the whole declaration incl. `;`, a definition's range starts at the return type and ends at `}`. Inconsistent with definitions' declarator-based ranges elsewhere. | `int fill(...);` 4:1-4:32 | check |
| 14 | Function child order | Body Block comes before the Parameters in `astChildren`. | typedef_over_builtin.c | by design? surprising for tools that walk children |
| 15 | Macro expansion | Nodes from an expanded macro all get the macro use's range, with code = the macro name; (a feature, replicated with expansion spans in the Rust preprocessor). | `if (SOME_MACRO)` -> BinaryOperator `<<` + 2 Literals all at 9:7-9:17 | by design |
| 16 | Invalid char literal | `'\90'` (non-octal escape) yields a ProblemExpression. | char_literal.c | probably fine |
| 17 | Designated initializers | Become `Assign` > `MemberAccess`/`Subscription`/`Range` > implicit `Reference` (loc `?`) to the variable. | designated.c | by design |
| 18 | Function pointer variable type | A variable of function pointer type has an EMPTY type name. | func_ptr_call.c: `void (*no_param)() = &target;` -> Variable `no_param` type `''` | **likely bug** (type name lost for FunctionPointerType) |
| 19 | Typedef resolution | Typedefs are resolved eagerly in the frontend (scope lookup while translating): `typedef int size_t; size_t p` -> Parameter type `int`, `typedef long int32_t` -> `long int`. Profile built-ins (`size_t`) are only a fallback. | typedef_over_builtin.c | by design (documented in #3060) |
| 20 | Comparison operators | `>=` and `!=` are typed `UNKNOWN`, while `<`, `<=`, `>`, `==` are `bool`. `Language.propagateTypeOfBinaryOperation` lists `"<", "=<", ">", "<=", "==", "==="`: a typo (`=<`) and `>=`/`!=` missing. **Confirmed with a probe** (`probe/cmp.c`): `a >= b` -> `UNKNOWN`, `a != b` -> `UNKNOWN`. | `int c = a >= b;` | **bug** (cpg-core Language.kt) |
| 21 | Call resolution without overloading | For C (no overloading) a single candidate by name is always the target, regardless of arguments; with several (redeclarations, or `g(int)`/`g(float)`) all viable ones are invoked, and the callee reference points at the first one with a body. A call that cannot be resolved leaves its callee reference unresolved too. | implicit_casts.c, stdlib_profile_own_definition.c | by design, note |
| 22 | `continue` in a `for` loop | The `continue` EOG edge goes to the loop's *condition*, skipping the iteration expression (`Loop.conditions` for `For` lists only the condition). In `for (...; i < n; i++) { if (c) continue; }` the `i++` is never reached from `continue`. **Confirmed with a probe** (`corpus2/loops.c`: Continue -> `i < n`, not `i++`). | `for (int i = 0; i < n; i++) { if (i == 2) continue; ... }` | **bug** (EvaluationOrderGraphPass.handleContainedBreaksAndContinues) |
| 23 | Non-deterministic callee/overload choice | With several candidate functions (prototype + definition, or overloads) the callee `Reference.refersTo` flips between runs (identity-set iteration order). 3 runs on the same input: `strlen` -> definition once, prototype twice; `implicit_casts.c` differs between consecutive runs. | stdlib_profile_own_definition.c, implicit_casts.c | **bug** (non-deterministic output) |
| 24 | Several forward gotos to one label | Only the LAST goto that jumps forward to a label is linked to it. Earlier ones lose both the name and the EOG edge (Goto name `''`, no `eog`). **Confirmed with a probe** (`corpus2/goto.c`). Backward gotos are fine. | `if (x) goto done; if (y) goto done; ... done:` | **bug** (single pending slot per label) |
| 25 | Unreachable code is not resolved | `SymbolResolver` follows the EOG, so references in code that the EOG never reaches (e.g. after `goto`/`return`/`break`) stay unresolved (`refersTo=-`, type UNKNOWN). | `goto end; r = 99; end:` -> `r` unresolved | by design, but surprising for dead code analysis |
| 26 | Pointer dereference naming | `PointerDereference` is named after its operand whatever that is (`*p++` -> name `++`). Member access via `->` is `MemberAccess(S::x)` over a `PointerDereference`. | `*p++`, `s->x` | note |
| 27 | Signed literals | `-1` is `UnaryOperator('-')` over `Literal(1)` in Kotlin (tree-sitter lexes it as one literal). | `return -1;` | by design |

## Known gaps of the tree-sitter side (not Kotlin issues)
- Implicit int (`main() { ... }` without a return type) is not parsed as a function. types.c.
- `(ident)` followed by `*p` is a Cast/Multiplication ambiguity (cast_to_inferred.c: CDT picks the multiplication with a `UnaryOperator '()'`).
- `(ident)(args)` is a call in tree-sitter, a Cast in CDT when `ident` is a type (cast_to_inferred.c). Needs the typedef/type-aware ambiguity step.

## To add while porting
- types of inferred functions (`printf(char[],int,int)UNKNOWN`) and parameter names (`charPtr0`, `int1`)
- anything found in ArrayConstruction / Subscription / Cast / InitializerList handling
