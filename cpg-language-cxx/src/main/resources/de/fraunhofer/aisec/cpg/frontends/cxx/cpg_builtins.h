/*
 * Compiler-intrinsic prelude that the CPG's CXX frontend prepends to every
 * translation unit (via ExtendedScannerInfo.includeFiles). This mirrors the
 * implicit typedefs and declarations clang/gcc inject into every TU that no
 * real header file provides.
 *
 * Ground truth is `clang -Xclang -ast-dump -fsyntax-only <empty>.c`; add an
 * entry here whenever an implicit builtin surfaces as an "inferred" record or
 * unresolved symbol in the CPG.
 *
 * Keep this file architecture-conditional via the __arch__ predefines that we
 * already pass into the scanner from the clang predefines snapshot.
 */

#ifndef _CPG_BUILTINS_H
#define _CPG_BUILTINS_H

#if defined(__arm64__) || defined(__aarch64__)
/*
 * arm64-apple-darwin: variadic arguments are passed entirely on the stack, so
 * clang models __builtin_va_list as a plain char * pointer. Verified with
 * `clang -Xclang -ast-dump`:
 *     TypedefDecl implicit referenced __builtin_va_list 'char *'
 */
typedef char *__builtin_va_list;
typedef char *__builtin_ms_va_list;
#endif

#endif /* _CPG_BUILTINS_H */
