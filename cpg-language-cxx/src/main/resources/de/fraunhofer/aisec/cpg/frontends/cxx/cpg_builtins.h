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

/*
 * clang implicit integer typedefs (`clang -Xclang -ast-dump`):
 *     TypedefDecl implicit __int128_t  '__int128'
 *     TypedefDecl implicit __uint128_t 'unsigned __int128'
 * CDT does not understand the `__int128` keyword, so we approximate with the
 * widest standard integer types available. This loses the exact 128-bit
 * width, but it is enough to keep typedef chains like Apple's `__darwin_*`
 * resolvable — no user code we care about here does 128-bit arithmetic.
 */
typedef long long __int128_t;
typedef unsigned long long __uint128_t;

/*
 * clang built-in functions referenced by the macOS math.h / string.h chain
 * (verified via `clang -Xclang -ast-dump -fsyntax-only`). Signatures match
 * clang's Builtins.def. Providing the prototypes here keeps the frontend
 * from having to infer a Function + implicit Parameters for each one.
 */
unsigned int __builtin_bswap32(unsigned int);
/* CDT normalises `unsigned long long` to `unsigned long long int` when it
 * compares parameter types for call resolution, so match that spelling here
 * to avoid a false-negative that would re-infer this builtin at the call
 * site with a slightly-different signature. */
unsigned long long int __builtin_bswap64(unsigned long long int);
float __builtin_fabsf(float);
double __builtin_fabs(double);
long double __builtin_fabsl(long double);
float __builtin_inff(void);
double __builtin_inf(void);
long double __builtin_infl(void);

/*
 * `_Float16` is a clang extension keyword (16-bit binary float). CDT does
 * not recognise it, and the macOS math.h chain references it only in a few
 * places (bounds-checking macros, IEEE 754 helpers). Aliasing to `float` is
 * a lossy approximation that keeps typedef chains parseable without pulling
 * in another compiler-specific keyword.
 */
typedef float _Float16;

#endif /* _CPG_BUILTINS_H */
