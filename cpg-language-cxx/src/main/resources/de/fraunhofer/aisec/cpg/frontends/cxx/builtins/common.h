/*
 * Cross-target clang built-in typedefs and function prototypes that don't
 * vary by architecture. Included by every per-target cpg_builtins.h.
 *
 * Ground truth for the type approximations is
 * `clang -Xclang -ast-dump -fsyntax-only <empty>.c`; ground truth for the
 * function prototypes is clang's Builtins.def. CDT does not understand a few
 * clang-only keywords (`__int128`, `_Float16`), so the corresponding
 * typedefs here are deliberately lossy approximations: they preserve the
 * typedef graph so user code parses, at the cost of the exact bit-width.
 */
#ifndef _CPG_BUILTINS_COMMON_H
#define _CPG_BUILTINS_COMMON_H

/* clang implicit integer typedefs (widest we can express through CDT). */
typedef long long __int128_t;
typedef unsigned long long __uint128_t;

/* clang extension keyword: 16-bit binary float. Aliased to `float` so the
 * bounds-checking helpers in the system math.h keep parsing. */
typedef float _Float16;

/* Selected clang built-in function prototypes referenced by the standard
 * headers. CDT normalises `unsigned long long` to `unsigned long long int`
 * for parameter matching, so spell it out here to avoid a re-inference at
 * the call site. */
unsigned int __builtin_bswap32(unsigned int);
unsigned long long int __builtin_bswap64(unsigned long long int);
float __builtin_fabsf(float);
double __builtin_fabs(double);
long double __builtin_fabsl(long double);
float __builtin_inff(void);
double __builtin_inf(void);
long double __builtin_infl(void);

#endif /* _CPG_BUILTINS_COMMON_H */
