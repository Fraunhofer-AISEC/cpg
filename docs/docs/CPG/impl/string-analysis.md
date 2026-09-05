# String Analysis

!!! note "Status"

    This document is a **design document**. It describes a planned component that is not
    implemented yet. It records the design decisions we took, so that the implementation can follow
    them, and so that later readers understand why the component looks the way it does.

## Motivation

A lot of interesting questions about a program are questions about a string:

* Which file does this program open? (`open(path)`)
* Which host does it connect to? (`requests.get(url)`)
* Which SQL statement is executed? (`cursor.execute(query)`)
* Which shell command is spawned? (`subprocess.run(cmd)`)
* Which configuration key is read? (`os.environ[key]`)
* Which logger is used, which message is logged?

We already have `ValueEvaluator` and `MultiValueEvaluator` for this. Both return a *concrete* value
(or a finite set of concrete values), and both give up as soon as the value is not fully determined:

* `ValueEvaluator.handlePrevDFG` aborts whenever a node has more than one incoming DFG edge, i.e.
  for every value that was written in more than one branch.
* Neither evaluator handles loops in a general way, so a string built up in a loop is lost.
* An unresolvable part is reported as the string `"{<node name>}"` by the default `cannotEvaluate`
  hook. That is indistinguishable from a literal of the same shape, and it throws away *why* the
  value is unknown.

In practice, most strings are *partially* known:

```python
def download(name, base="https://example.com/"):
    if name.endswith(".tar.gz"):
        suffix = "archives/"
    else:
        suffix = "files/"
    return base + suffix + name          # <-- what is this?
```

The answer we want here is not "unknown", it is
`https://example\.com/(archives/|files/).*` — a **regular expression** with holes where the value is
genuinely undetermined. That is what this component provides.

The use case is deliberately general: concept passes, Codyze rules, the query API, MCP-based
exploration and interactive graph inspection all want the same thing. This is not a
malware-analysis-specific feature.

## Goals and non-goals

**Goals**

* A string abstract domain that is *closed* under the operations the CPG produces: joins from
  branching data flow, loops, recursion, unknown inputs.
* A demand-driven, backward evaluator: "give me everything you know about the value of *this* node",
  callable from anywhere (passes, queries, tests), without requiring a whole-program pre-analysis.
* **Interprocedural by default**, using the existing context-sensitive DFG edges.
* Explainable results: every hole knows which node it came from, and every over-approximation is
  recorded as an `Assumption`.
* A readable rendering as a regular expression, plus programmatic accessors (`asConstant`,
  `possiblePrefixes`, `mayMatch`, `mustMatch`, …).

**Non-goals (for now)**

* Solving for inputs, i.e. "which value of `argv[1]` makes this open `/etc/shadow`". That is the
  pre-image/backward-reachability problem (see [Related work](#related-work)) and needs transducers
  or a string solver.
* Full precision for `replace`-like operations. These need string transducers; we over-approximate
  and record an assumption.
* Context-free precision. We approximate everything regularly.

## Related work

The design borrows from the following lines of work. It is worth reading this section before
changing the domain, because most of the obvious "improvements" have known trade-offs.

**Cheap non-relational domains.** Constant propagation, prefix/suffix, character inclusion (which
characters must/may occur), length intervals, character sets. Catalogued by Costantini, Ferrara and
Cortesi, *A suite of abstract domains for static analysis of string values* (SPE 2015). Cheap and
always terminating, but they cannot express "`/tmp/` followed by something followed by `.log`", which
is exactly the shape we care about.

**Regex-like algebraic domains.** The **bricks** domain and the **string graph** domain from the same
line of work are essentially regular expressions with holes, equipped with lub, glb and widening.
This is the closest match to what we want and the cheapest representation that produces a readable
result.

**Automata-based.** The mainstream of the field:

* Christensen, Møller and Schwartzbach, *Precise Analysis of String Expressions* (SAS 2003) — the
  Java String Analyzer. Builds a flow graph of string expressions, derives a context-free grammar,
  approximates it regularly (Mohri–Nederhof) and converts it to a DFA.
* Yu, Alkhalaf and Bultan, *Stranger* (TACAS 2010) and *Generating vulnerability signatures for
  string manipulating programs using automata-based forward and backward symbolic analyses*
  (ASE 2009). Symbolic automata, **automata widening** for loop convergence, and symbolic string
  transducers for `replace`. This is the reference for genuine backward (pre-image) string analysis.
* Negrini, Arceri, Ferrara and Cortesi, *Twinning automata and regular expressions for string static
  analysis* (VMCAI 2021) — **Tarsis**. Finite automata whose alphabet consists of *whole strings*
  rather than single characters, plus an explicit `TOP` symbol for unknown segments. Far more
  scalable than character-level automata, and the output reads like a regex with holes. This is the
  single most transferable design for our purposes, and the reason our alphabet is whole strings.
* Hooimeijer, Livshits, Molnar, Saxe and Veanes, *Fast and precise sanitizer analysis with BEK*
  (USENIX Security 2011) — symbolic finite transducers for sanitizer modelling.

**Grammar-based.** Minamide, *Static approximation of dynamically generated web pages* (WWW 2005) and
Wassermann and Su (PLDI 2007). More precise than regular for recursive generation, but harder to
join and to render.

**Logic/solver-based.** Tateishi, Pistoia and Tripp, *Path- and index-sensitive string analysis based
on monadic second-order logic* (ISSTA 2011). Very precise; a poor fit for "always return something
useful for any query on any program".

**Slice-and-execute.** Widely used in the Android/binary world: *Harvester* (Rasthofer et al.,
NDSS 2016) and *StringHound* (Glanz et al., ICSE 2020) compute a backward slice and then concretely
execute it; *IC3/COAL* (Octeau et al., ICSE 2015) performs multi-valued composite constant
propagation. Cheap and effective but unsound; complementary to this component rather than a
replacement.

## Design decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | Represent values as a **term algebra** (`Const`/`Concat`/`Union`/`Star`/`Unknown`), not as automata, in the first iteration. | Produces a directly readable regex, needs no new code for determinisation/minimisation, and is enough for the common "partially known string" case. |
| D2 | The alphabet is **whole strings**, not characters (Tarsis-style). | Character-level representations blow up for long literals and produce unreadable output. |
| D3 | **Automaton backing is a planned second iteration**, behind the same public API. See [Future work](#future-work-automaton-backing). | We will eventually need exact language inclusion, intersection and complement, which a term algebra cannot give us soundly. The API is designed so this is an internal change. |
| D4 | **No new third-party dependencies.** When we add the automaton backing, we implement it in-repo. | Project-wide constraint. `dk.brics.automaton` would have been the obvious choice; we deliberately do not use it. |
| D5 | The component lives in **`cpg-analysis`**, package `de.fraunhofer.aisec.cpg.analysis.string`. | It is an analysis, not a graph feature, and it reuses `LatticeInterval` which already lives there. See the consequence in [Module placement](#module-placement). |
| D6 | **Interprocedural by default**, via the existing `Interprocedural` scope and `ContextSensitive` sensitivity. | Most real strings are assembled across function boundaries; an intraprocedural default would make the component useless for the majority of queries. Budgets are configurable. |
| D7 | The primary entry point is a **demand-driven backward evaluator**, not a pass. | Callers ask about single nodes. A whole-program forward fixpoint is added later, for the cases the backward evaluator handles badly (see [Phase 5](#phase-5-flow-sensitive-variant)). |
| D8 | `StringPattern` implements `Lattice.Element` from the start. | Makes it directly usable inside `HashMapLattice`, `ConcurrentMapLattice` and `TupleLattice` for the flow-sensitive variant, without a second representation. |
| D9 | Imprecision is recorded via the existing `Assumption` mechanism, not just logged. | Consistent with the rest of the codebase; lets consumers report *why* a result is coarse. |

### Module placement

`cpg-analysis` has `api(projects.cpgConcepts)`, i.e. it depends *on* `cpg-concepts`. Consequently
the concept passes in `cpg-concepts/src/main` **cannot** consume this component. Places like
`PythonStdLibConfigurationPass` and `PythonLoggingConceptPass`, which today do
`evaluate() as? String` and lose everything when that fails, therefore cannot be migrated while the
component sits in `cpg-analysis`.

This is an accepted consequence of D5. Consumers in the first iterations are the query API,
`codyze-core` (which depends on `cpg-analysis`) and user code. If we later want concept passes to
use string patterns, the domain (not necessarily the evaluator) has to move to `cpg-core`, next to
`ValueEvaluator`. The public API is kept free of `cpg-analysis`-only types so that this move stays
mechanical.

## The domain

```kotlin
package de.fraunhofer.aisec.cpg.analysis.string

/**
 * An abstraction of the set of strings a node may evaluate to. Terms are immutable and always kept
 * in a normalised form, see [normalize].
 */
sealed interface StringPattern : Lattice.Element {
    /** No value at all: unreachable, or nothing is known to flow here. Bottom of the lattice. */
    object Bottom : StringPattern

    /** Exactly one known string. */
    data class Const(val value: String) : StringPattern

    /** Sequence of patterns. Never nested, never contains [Bottom], adjacent [Const]s are merged. */
    data class Concat(val parts: List<StringPattern>) : StringPattern

    /** Alternatives, e.g. from a branching DFG or a [Conditional]. Never empty, never singleton. */
    data class Union(val alternatives: Set<StringPattern>) : StringPattern

    /** [inner] repeated between [min] and [max] times ([max] `== null` means unbounded). */
    data class Star(val inner: StringPattern, val min: Int = 0, val max: Int? = null) : StringPattern

    /**
     * An undetermined segment. [origin] records *why* it is unknown - a parameter, a file read, a
     * call we cannot model - which is what makes results explainable. [charSet] and [length] refine
     * it when we know something.
     */
    data class Unknown(
        val origin: Node? = null,
        val reason: Reason = Reason.UNSUPPORTED,
        val charSet: CharSet = CharSet.Any,
        val length: LatticeInterval = LatticeInterval.TOP,
    ) : StringPattern

    enum class Reason { PARAMETER, EXTERNAL_INPUT, UNSUPPORTED, BUDGET_EXCEEDED, WIDENED }
}
```

`Top` is `Unknown(origin = null, charSet = CharSet.Any, length = LatticeInterval.TOP)` — no separate
object, so that refinement is uniform.

`CharSet` is a small, dependency-free domain:

```kotlin
sealed interface CharSet {
    object Empty : CharSet
    /** Explicit set, capped at [MAX_EXPLICIT] characters, above which we go to [Any]. */
    data class Chars(val chars: Set<Char>) : CharSet
    object Any : CharSet
}
```

`length` reuses the existing `LatticeInterval` from
`de.fraunhofer.aisec.cpg.analysis.abstracteval` — including its `widen`/`narrow` — which is one of
the reasons for D5.

### Normalisation

Every constructor result goes through `normalize`, which establishes a canonical form:

1. Flatten nested `Concat` and nested `Union`.
2. Drop `Const("")` from `Concat`; merge adjacent `Const`s.
3. `Concat` containing `Bottom` becomes `Bottom`; `Union` drops `Bottom` alternatives.
4. Collapse singleton `Concat`/`Union` to their element.
5. Sort `Union` alternatives by a stable total order (so that structural equality is meaningful).
6. Deduplicate `Union` alternatives; if an alternative subsumes another, drop the subsumed one.
7. Factor out common prefixes and suffixes of a `Union` where cheap
   (`{"ab", "ac"} -> Concat(Const("a"), Union{Const("b"), Const("c")})`). This keeps output readable
   and keeps term size down.
8. If the `Union` has more than `maxUnionSize` alternatives, or the term exceeds `maxTermSize`
   leaves, collapse to `Unknown` with the joined `charSet` and `length` of the alternatives.

**Invariant:** two normalised terms denote the same language *if* they are structurally equal. The
converse does not hold (we may fail to notice that two different terms are equivalent) — that costs
precision, never soundness. This is the central limitation of the term-only iteration and the main
motivation for D3.

### Lattice

```kotlin
class StringLattice : Lattice<StringPattern>, HasWidening<StringPattern>
```

* `bottom` = `StringPattern.Bottom`.
* `lub(a, b)` = `normalize(Union(a, b))`. Cheap and exact.
* `glb(a, b)` = exact for the easy cases (`Bottom`, equal terms, `Const` vs `Const`, `Const` against
  an `Unknown` whose `charSet`/`length` admit it), otherwise the sound fallback: the more specific of
  the two if one syntactically subsumes the other, else `Bottom`. An exact `glb` needs intersection,
  i.e. the automaton backing.
* `compare(a, b)` is derived from a `subsumes(a, b)` relation that is *sound in one direction*: when
  it returns `true`, language inclusion really holds; when it returns `false`, we answer
  `Order.UNEQUAL`. Because of the normalisation invariant, `Order.EQUAL` is reliable, which is what
  the fixpoint iteration in `Lattice.iterateEOG` needs for termination detection.
* `duplicate` returns the receiver — terms are immutable.

### Widening

Widening is what makes loops and recursion terminate, and it is the part to get right:

```
widen(one, two):
  j = lub(one, two)
  if size(j) <= maxTermSize && depth(j) <= maxTermDepth: return j
  // generalise instead of growing:
  if two == Concat(one, x):        return Concat(one, Star(x))          // append-in-a-loop
  if two == Concat(x, one):        return Concat(Star(x), one)          // prepend-in-a-loop
  return Unknown(
      origin = commonOrigin(one, two),
      reason = WIDENED,
      charSet = charSet(one) union charSet(two),
      length  = length(one).widen(length(two)),
  )
```

Termination argument: once a term exceeds the size bound it is replaced by either a `Star` term
(whose inner term is strictly smaller) or an `Unknown`, whose two components (`CharSet` with its
finite height, `LatticeInterval` with its own widening) both stabilise. Ascending chains therefore
have bounded length. This must be covered by explicit tests.

### Rendering

* `fun StringPattern.toRegexString(): String` — the readable form used in reports and tests:
  `Const` escaped, `Concat` juxtaposed, `Union` as `(a|b)`, `Star` as `(x)*`/`(x){2,}`, `Unknown` as
  a character class plus quantifier derived from `charSet` and `length` (`.*`, `[0-9]{1,4}`, …).
* `fun StringPattern.toRegex(): Regex` — the compiled, anchored form for matching.
* `override fun toString()` delegates to `toRegexString()`, so patterns are legible in debuggers,
  logs and query trees.

### Query helpers

```kotlin
fun StringPattern.asConstantOrNull(): String?           // exactly one value
fun StringPattern.enumerate(limit: Int): Set<String>?   // finite language, or null
fun StringPattern.mayMatch(regex: Regex): Boolean       // may-analysis
fun StringPattern.mustMatch(regex: Regex): Boolean      // must-analysis (conservative without D3)
fun StringPattern.constantPrefix(): String              // longest known prefix, possibly ""
fun StringPattern.constantSuffix(): String
fun StringPattern.mustContain(): Set<String>            // substrings present on every path
val StringPattern.isFullyKnown: Boolean                 // no Unknown, no Star
val StringPattern.unknownOrigins: Set<Node>             // for explanations
```

`mustMatch` is the one that is genuinely limited by the term-only representation: without
complementation we can only answer it for patterns we can enumerate. It returns a conservative
`false` otherwise, and records an assumption. This is documented on the function.

## The evaluator

```kotlin
class StringEvaluator(val config: StringEvaluatorConfig = StringEvaluatorConfig()) {
    fun evaluate(node: Node): StringPattern
}

data class StringEvaluatorConfig(
    /** Interprocedural by default (D6). */
    val scope: AnalysisScope = Interprocedural(maxCallDepth = 10, maxSteps = 5_000),
    val maxTermSize: Int = 64,
    val maxTermDepth: Int = 16,
    val maxUnionSize: Int = 16,
    /** Follow calls into inferred functions? Off by default: nothing useful to be learned there. */
    val enterInferredFunctions: Boolean = false,
)
```

Structurally this mirrors `ValueEvaluator`: a `when` over node types with `open` handler methods, so
that languages can extend it. The differences from `ValueEvaluator` are the important part:

1. **Joins produce `Union` instead of aborting.** Where `ValueEvaluator.handlePrevDFG` gives up on
   `prevFullDFG.size > 1`, we take the `lub` of all predecessors.
2. **Predecessors are chosen context-sensitively.** Rather than reading `node.prevFullDFG` directly,
   the evaluator delegates to
   `Backward(GraphToFollow.DFG).pickNextStep(node, config.scope, ctx, path, loopingPaths, OnlyFullDFG + ContextSensitive)`.
   This reuses the machinery that `followPrevFullDFGEdgesUntilHit` is built on, and gives us D6 for
   free: `ContextSensitiveDataflow` edges with `CallingContextIn`/`CallingContextOut` already connect
   arguments to parameters and returns to call sites, and the `Context.callStack` keeps us from
   returning into the wrong caller. Budgets come from `AnalysisScope` (`maxCallDepth`, `maxSteps`)
   instead of a hand-rolled depth counter.
3. **Cycles widen instead of failing.** Re-reaching a node on the current path yields
   `widen(previousResultForThatNode, current)` — the `Star` case above — instead of
   `cannotEvaluate`.
4. **Unknowns are first-class.** A `Parameter` with no incoming edge in the current context becomes
   `Unknown(origin = parameter, reason = PARAMETER)`, not a fake literal.
5. **Budget exhaustion is a value, not a failure.** Exceeding `maxSteps`/`maxCallDepth`/`maxTermSize`
   yields `Unknown(reason = BUDGET_EXCEEDED)` plus an assumption, so a partial result survives.

Handlers to implement, in order of value:

| Node | Handling |
|---|---|
| `Literal<*>` | `Const(value.toString())` for string/char literals, `Bottom` for a `null` literal |
| `BinaryOperator` `+`/`+=` | `Concat(lhs, rhs)`. Covers Python f-strings for free: `ExpressionHandler.handleJoinedStr` already lowers them into `+` trees |
| `Assign` | rhs, or `computeBinaryOpEffect` for compound assignments |
| `Conditional` | `Union(then, else)`, refined by a constant-folded condition where possible |
| `Cast` | inner expression |
| `Reference`, `Variable`, `Field` | context-sensitive predecessors |
| `Call` | language-specific handler (below), default: predecessors of the return value |
| `Subscription` | index/slice on a known `Const` or `InitializerList`, else `Unknown` |
| `Parameter` | call-site arguments via `CallingContextIn`, else `Unknown(PARAMETER)` |
| `UnaryOperator` `*`/`&` | inner expression, as in `ValueEvaluator` |

### Language extension points

`Language.evaluator` (`cpg-core`) cannot be reused, because that property is typed `ValueEvaluator`
and lives in a module that cannot see `cpg-analysis`. Instead, language-specific behaviour is
registered in `cpg-analysis` via a small registry keyed by language:

```kotlin
interface StringOperationHandler {
    /** Returns `null` if this handler does not know the call, so the next one is tried. */
    fun handleCall(call: Call, evaluate: (Node) -> StringPattern): StringPattern?
}
```

Planned handlers, in priority order:

* **Python**: `str.format`, `%`, `str.join`, `os.path.join` (already partly modelled in
  `PythonValueEvaluator.handleCall`), `str.replace`, `strip`/`lstrip`/`rstrip`, `upper`/`lower`,
  slicing, `encode`/`decode`.
* **JVM/Java**: `StringBuilder.append` chains (the classic case), `String.format`, `String.join`,
  `concat`, `substring`, `replace`.
* **C/C++**: `strcat`, `strncat`, `snprintf`, `strcpy` — over-approximated where the buffer aliasing
  is unclear.
* **Language-agnostic**: base64/hex encode and decode, since they appear in every language.

`replace`-family operations are approximated: `s.replace(a, b)` becomes
`Concat(prefix(s), Top)` when `s` is not fully known, or is computed exactly when `s`, `a` and `b`
are all constants. Every inexact case records an `Assumption`:

```kotlin
result.assume(
    AssumptionType.SoundnessAssumption,
    "We assume that the result of the call to `replace` at ... is over-approximated by `.*`. " +
        "To verify this assumption, we need to check ...",
    scope = call,
)
```

### Public API

```kotlin
// de.fraunhofer.aisec.cpg.analysis.string
fun Node.evaluateString(config: StringEvaluatorConfig = StringEvaluatorConfig()): StringPattern
```

plus query-API integration in `cpg-analysis/query`, returning `QueryTree`s so that results compose
with the rest of the query language and carry their assumptions:

```kotlin
fun Node.stringValue(): QueryTree<StringPattern>
fun Node.stringMustMatch(regex: Regex): QueryTree<Boolean>
fun Node.stringMayMatch(regex: Regex): QueryTree<Boolean>
```

## Implementation plan

### Phase 1: the domain

*No graph involvement, fully unit-testable, can land on its own.*

* `StringPattern`, `CharSet`, `normalize`, `subsumes`.
* `StringLattice` with `lub`/`glb`/`compare`/`duplicate`/`widen`.
* `toRegexString`, `toRegex`, and the query helpers.
* Files: `cpg-analysis/src/main/kotlin/de/fraunhofer/aisec/cpg/analysis/string/StringPattern.kt`,
  `CharSet.kt`, `StringLattice.kt`, `Rendering.kt`.
* Tests modelled on `LatticeIntervalTest`: lattice laws (commutativity, associativity, idempotence
  of `lub`; `compare` consistent with `lub`/`glb`), normalisation canonicity, widening termination
  on synthetic ascending chains, `Const -> regex -> matches` round-trips.

### Phase 2: the backward evaluator

* `StringEvaluator`, `StringEvaluatorConfig`, the handler `when`, the `pickNextStep`-based
  predecessor lookup, cycle widening, budget handling, assumption recording.
* `Node.evaluateString()`.
* Tests: small snippets per construct (branching assignment, loop append, conditional, parameter
  passing, cross-function flow), each asserting the rendered regex.

### Phase 3: language operations

* The `StringOperationHandler` registry and the Python handler first, since Python is the
  best-covered frontend and the concept passes are the loudest consumers.
* Then JVM, then C/C++.

### Phase 4: consumers

* Query-API helpers and their tests.
* Migrate the `evaluate() as? String` sites that are *reachable* from `cpg-analysis` (see
  [Module placement](#module-placement)) so that a partially-known string yields a pattern-valued
  result instead of nothing.

### Phase 5: flow-sensitive variant

Only worth doing once phases 1–3 are in use and we know which cases the backward evaluator handles
badly (loop-built strings, path-dependent values are the expected ones).

* Prerequisite refactor: `TupleState` and `DeclarationState` in `AbstractIntervalEvaluator.kt` are
  hard-wired to `NewIntervalLattice.Element`. Generify them over the inner element type. This
  unblocks any future `Value<T>` domain, not just this one.
* Then `StringValue : Value<StringPattern>` and an `AbstractStringEvaluator` driven by
  `Lattice.iterateEOG` with `Strategy.WIDENING_NARROWING`, mirroring `AbstractIntervalEvaluator`.
  D8 means the same `StringPattern` type is used, with no conversion.

### Phase 6: soundness testing

Beyond per-construct unit tests:

* **Differential**: wherever `ValueEvaluator` produces a `String`, the pattern must match it. This is
  a cheap and very effective regression net, runnable over all existing test fixtures.
* **Concrete execution**: for small Python fixtures, run them and assert that every observed string
  is matched by the computed pattern.
* **Property-based**: generate random terms, check the lattice laws and that `lub` never shrinks the
  language (`a subsumes-> lub(a,b)`).
* **Performance**: a benchmark on a large fixture, guarding against the budget defaults being too
  generous.

## Future work: automaton backing

The second iteration replaces the *internals* of `subsumes`, `glb` and `mustMatch` with an automaton
representation, keeping `StringPattern` and the public API as they are:

* A hand-written NFA/DFA over an alphabet of **whole strings** plus a `TOP` symbol (Tarsis-style),
  implemented in `cpg-analysis` — no new dependency (D4).
* Needed operations: Thompson construction from a `StringPattern`, epsilon-closure, subset
  construction, minimisation, product for intersection, complement for `mustMatch`, and
  language-inclusion testing.
* Regex extraction (state elimination) for rendering, so that output stays readable.
* At that point `compare` becomes exact, `glb` becomes real intersection, `mustMatch` becomes
  decidable, and the size-based collapse in normalisation can be replaced by automata widening
  (Bartzis–Bultan) which loses much less precision.

Open questions for that iteration, to be answered when we get there:

* Do we keep the term representation as the *canonical* one and use automata only as a decision
  procedure (simpler, keeps output readable), or do we switch representation entirely (more precise,
  needs good regex extraction)? The former is the current expectation.
* Do we need transducers for `replace`, or is the constants-only exact case plus over-approximation
  good enough in practice? Answer this from measurements, not from first principles.

## Risks

* **Precision cliff from over-eager collapsing.** `maxUnionSize`/`maxTermSize` are the knobs that
  decide whether a result is useful or is `.*`. They need to be measured, not guessed.
* **Interprocedural fan-out.** D6 means a query on a widely-used utility function unions over all
  call sites. `maxCallDepth`/`maxSteps` bound it, but the defaults will need tuning, and results may
  need caching per `(Node, Context)` — note that `ValueEvaluator`'s cache is keyed on `hashCode`
  alone, which is not adequate here.
* **`compare` incompleteness** (the normalisation invariant only holding in one direction) slows
  fixpoint convergence in phase 5. Widening covers termination, but watch the iteration counts.
* **Thread safety.** Evaluators are shared and invoked concurrently from parallel query evaluation —
  the reason `ValueEvaluator.path` is a `ThreadLocal`. `StringPattern` being immutable avoids most of
  this; any mutable evaluator state must follow the same discipline.
