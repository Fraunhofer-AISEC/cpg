# cpg-verified

Formally verified translation of source code ASTs into CPG nodes, written in
[Lean 4](https://lean-lang.org).

Today, language frontends build the CPG imperatively in Kotlin handlers. This project explores
replacing (parts of) them with translations that are pure functions written in Lean, together
with machine-checked proofs that they are correct. Because the translation itself is the Lean
code, the proofs cover exactly the code that will run (compiled to a native library and called
from the JVM, like the Go frontend's `libgoast`).

Only the AST-to-CPG step of the frontends is in scope; edges added by later passes (EOG, DFG,
symbol and call resolution, ...) are not.

## Status

First slice: Go expressions — basic literals, identifiers, binary and unary operators,
parentheses and calls. Every other expression kind is translated into a `ProblemExpression`.

| File                             | Content                                                       |
|----------------------------------|---------------------------------------------------------------|
| `CpgVerified/Cpg/Expr.lean`      | Model of the CPG expression node classes                      |
| `CpgVerified/Cpg/Semantics.lean` | Language-independent evaluator for CPG expressions            |
| `CpgVerified/Go/Ast.lean`        | Model of `go/ast` expressions                                 |
| `CpgVerified/Go/Literal.lean`    | Values of Go literals according to the Go spec                |
| `CpgVerified/Go/Semantics.lean`  | Evaluator for Go expressions according to the Go spec         |
| `CpgVerified/Go/Translate.lean`  | The translation (counterpart of `ExpressionHandler`)          |
| `CpgVerified/Go/Correctness.lean`| Proofs                                                        |

### Proven

* `Go.translate_correct`: **semantic preservation**. Evaluating the CPG produced for a Go
  expression yields exactly the value (or failure) that the Go semantics gives the expression,
  provided that variables and functions agree up to name resolution (`EnvAgree`) and that the
  context correctly states which of the predeclared identifiers `true`, `false`, `nil`, `iota`,
  `new` and `make` are shadowed by a declaration.
* `Go.translate_loc`: every translated node carries the location of its source node.
* Totality and termination of the translation (enforced by Lean for every definition).

`Axioms.lean` prints the axioms these results rely on; it must only list Lean's standard axioms.

### Findings in the Kotlin `ExpressionHandler`

Writing the specification revealed the following bugs in the Go frontend (all fixed):

* `0o17` and legacy octal literals such as `017` were parsed as decimal, and upper-case prefixes
  (`0X1F`) failed to parse.
* Rune literals got the opening quote character as their value instead of the code point, and
  escape sequences in rune and string literals were not decoded.
* `iota` outside of a constant declaration got a stale value of the last constant declaration;
  Go rejects it.
* Predeclared identifiers such as `true` were always translated to literals, even if they are
  shadowed by a local declaration.
* `make` dropped its last argument, and every call to a function or method called `new` or
  `make` (e.g. `t.make()`) was treated as the built-in.

### Trusted base

* Lean's kernel (checks the proofs).
* Lean's compiler and runtime (execute the translation; not verified), and the C interface in
  `native/cpg_verified.c`.
* The Go parser, and the (de)serialization between it, Lean and the Kotlin node classes.
* The modelling choices: integers are mathematical integers (no overflow), and the semantics of
  the CPG are given by `Cpg/Semantics.lean`.

## Running on Go files

The translation is called from the JVM in-process. `native/build.sh` builds a shared library
(`.lake/build/native/libcpgverified.{dylib,so}`) that statically links the compiled Lean code and
the Lean runtime, so it does not need a Lean installation at runtime. `native/cpg_verified.c` is its
C interface: it initializes the Lean runtime and passes byte buffers to `Wire.translateBytes`.
Requests and results are canonical S-expressions (`CpgVerified/Wire`). The same translation is
also available as a standalone executable, `cpg-translate`, which reads requests from standard
input.

`VerifiedTranslationTest` in `cpg-language-go` loads the library via JNA (`LeanTranslator`) to
compare the verified translation with the Go frontend: it parses every Go file in the frontend's
test resources, sends each value expression together with its context (name scope, imported
packages, `iota`, shadowed predeclared identifiers) to the library, and compares the result with
the CPG nodes that the Go frontend produces at the same location (without passes). Sub-expressions
outside the verified subset are skipped. The test is skipped if the library has not been built.

```bash
cpg-verified/native/build.sh
./gradlew :cpg-language-go:test --tests "*VerifiedTranslationTest*"
```

## Building

Install [elan](https://github.com/leanprover/elan) (e.g. `brew install elan-init`), then:

```bash
cd cpg-verified
lake build                    # builds and checks all proofs
lake test                     # runs the compiled translation on test inputs (Tests/Main.lean)
lake env lean Axioms.lean     # lists the axioms the main theorems depend on
```
