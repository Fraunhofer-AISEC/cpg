# CPG / Codyze live-demo kit

This folder contains four small example projects and a tutorial script for a
~30-minute live demo of the CPG and Codyze. Every command below is
copy-paste-ready; every query has been verified against the bundled examples.

> **Prerequisite:** the codyze CLI is built. From the repo root:
>
> ```bash
> ./gradlew :codyze:installDist -x pnpmInstall -x pnpmBuild \
>     -x :codyze-console:processResources \
>     -x :codyze-console:processIntegrationTestResources \
>     -x :codyze-console:processTestResources
> ```
>
> Then put the binary on your `PATH` (or use the full path):
>
> ```bash
> alias codyze=$PWD/codyze/build/install/codyze/bin/codyze
> ```

Each section below is a self-contained block. Open the REPL on the indicated
project, run the query, talk through it.

---

## Demo arc at a glance

|          Time | Section                                  | Example             | Take-away                                           |
| ------------: | ---------------------------------------- | ------------------- | --------------------------------------------------- |
|   0:00 – 5:00 | Warm-up: the CPG is a queryable database | `01-c-pointer-vuln` | Code is a graph. We can query it like one.          |
|  5:00 – 17:00 | Pointer-aware bug hunting in C           | `01-c-pointer-vuln` | UAF, double-free, buffer overflow as graph queries. |
| 17:00 – 22:00 | Same queries, different language         | `02-python-flow`    | One API, every supported frontend.                  |
| 22:00 – 28:00 | From query to policy: Codyze compliance  | `04-compliance`     | Queries become signed requirements.                 |
| 28:00 – 30:00 | Open REPL / Q&A                          | any                 | Audience drives.                                    |

The clickable `file:line` hyperlinks in REPL output rely on **OSC 8** —
they work in iTerm2, Ghostty, WezTerm, and recent VS Code integrated
terminals. Outside of those they show as plain text.

**Line-jump:** the REPL auto-detects VS Code (via `TERM_PROGRAM=vscode`)
and emits `vscode://file<path>:<line>:<col>` URLs that jump to the exact
line. In other terminals it falls back to `file://<path>#L<line>`, which
opens the file but cannot navigate (the OS handler strips the fragment).
Override the scheme with `CODYZE_LINK_SCHEME=vscode|file|none`.

---

## 0:00 – 5:00 — Warm-up: "the graph knows your code"

> **Project:** `examples/01-c-pointer-vuln/` — one C file. Don't tell the
> audience yet that it's full of bugs; right now we're just exploring.

Open the REPL:

```bash
codyze repl examples/01-c-pointer-vuln
```

The CODYZE banner prints first, then the analyzer runs for a few seconds,
then you land in `codyze>`. While analysis runs, narrate: _"Codyze is
parsing this C file with the CPG — Code Property Graph. By the time the
prompt drops, the entire codebase will be available as a queryable
graph in Kotlin."_

### Step 1 — Size up the project

```kotlin
result.components
result.functions.size
result.functions
```

You should see **1 component**, then `11` functions: the six we wrote in
`vuln.c`, plus `main`, plus inferred declarations for the libc calls
(`malloc`, `strcpy`, `free`, `printf`). Narrate: _"Even though we didn't
hand it any header files, the CPG inferred the libc symbols from how
they were used."_

### Step 2 — Drill into one function

```kotlin
val main = result.functions["main"]
main
```

The single `Function` node renders bold with a clickable `vuln.c:62`
link. **Click it** — your editor opens `vuln.c` at line 62.

Now press TAB after `main.` and watch the menu — properties like
`parameters`, `body`, `prevDFG`, `nextEOG`, `code`, `location`, plus
extension methods (`callersOf(...)`, `controlledBy()`, …). _"This is the
shortcut API: everything you can ask about a node is right here."_

```kotlin
main?.body
main?.code
```

The second one prints the literal source of `main()` from the file.

### Step 3 — Find all calls to a specific function

```kotlin
result.calls("free")
```

Five `free` calls show up, each with a clickable file:line. **Click two
or three** to show the audience that the graph has full positional
fidelity.

Now look closely at the list. Two of them are on **lines 31 and 32** —
right next to each other. **Click line 31**:

```c
void double_free(void) {
    char *p = malloc(64);
    free(p);
    free(p);                    /* BUG: second free */
}
```

…that's `free(p); free(p);` in the same function. Don't say "double-free"
out loud yet — just let the audience notice. _"Interesting. Same pointer,
two consecutive frees. We'll come back to that in a minute."_

This is the moment the talk turns: the graph already surfaced something
suspicious without us asking a security question. We just listed `free`
calls.

### Step 4 — Follow a value through the graph

Let's zoom into a single function. Notice how natural the lookup syntax
is — collections of nodes can be indexed by name:

```kotlin
val uaf = result.functions["uaf_simple"]
val m = uaf.calls["malloc"]!!
val f = uaf.calls["free"]!!
m
f
```

Both `m` and `f` render as `Call` nodes with their clickable file:line
links (`vuln.c:11` and `vuln.c:13`).

Now ask the graph a real question: _"Where does the value returned by
`malloc` flow next?"_

```kotlin
m.nextDFG
```

You get one downstream node: `p : Variable` at `vuln.c:11` — the local
variable that captures the malloc's return value.

And the punchline: _"Can the value from `malloc` reach the `free` call?"_

```kotlin
dataFlow(startNode = m, predicate = { it == f }).value
```

`true`. The CPG followed the value across the assignment to `p`, through
the `strcpy` call (which doesn't sever the data flow), all the way to
`free(p)`. _"This is the data-flow graph at work — value-level reasoning
that just tracking syntax can't do."_

### Step 5 — Show the path, not just the verdict

Drop the `.value`:

```kotlin
dataFlow(startNode = m, predicate = { it == f })
```

Now the REPL renders the full `QueryTree` as an indented tree with every
step of the path expanded:

```
✓ [ANY] data flow from malloc → free            vuln.c:11
└─ ✓ [EVALUATE] fulfills the requirement       vuln.c:11
   ├─ (6 node(s))
   │  ├─ step 1: malloc   : Call         vuln.c:11
   │  ├─ step 2: p        : Variable     vuln.c:11
   │  ├─ step 3: p        : Reference    vuln.c:13
   │  ├─ step 4: charPtr0 : Parameter    (inferred)
   │  ├─ step 5: free     : Function     (inferred)
   │  └─ step 6: free     : Call         vuln.c:13
```

Every step is a clickable node. Note the `(inferred)` tag — those nodes
were synthesised by the CPG (libc symbols seen via use, no source line),
so they have no link. **Glance through the path** to show the audience
the chain of reasoning, then move on. (For a richer, navigable view we
export to SARIF in the next step.)

### Step 6 — Open the path in your editor

The tree is informative but text-only. To show the path **inside VS
Code** with a clickable side-panel and per-step navigation, export it as
SARIF:

```
:flow
```

The REPL writes a temporary `.sarif` describing the codeflow and asks the
OS to open it. With the **SARIF Viewer** extension
(`MS-SarifVSCode.sarif-viewer`) installed, the file opens in a side
panel listing each step with its source location; clicking a step jumps
the editor to that line.

> The REPL routes through `vscode://file/<sarif-path>` when it detects
> it's running inside VS Code (via `TERM_PROGRAM=vscode`), so the file
> always lands in VS Code — never your browser or some other registered
> `.sarif` handler.

_"That's the path the CPG just discovered, exported as standard SARIF —
the same format your CI tools already understand."_

### The pivot

You've spent five minutes treating the file like a database of code
facts. Drop the line:

> _"We've not asked anything about security yet. We just looked at the
> shape of the code. Now watch the same API answer security questions —
> use-after-free, double-free, buffer overflow — without changing
> tools, languages, or even Kotlin syntax style."_

---

## 5:00 – 17:00 — Pointer-aware bug hunting

Stay in the same REPL (still on `01-c-pointer-vuln`). The file deliberately
contains:

- `uaf_simple` — classic use-after-free
- `uaf_aliased` — UAF hidden behind `char *q = p;`
- `double_free` — `free(p); free(p);`
- `buffer_overflow` — `strcpy(buf, user_input)` into an 8-byte stack buffer
- `leak` — `malloc` with no matching `free`
- `safe` — the same allocation pattern done right (negative control)

### Use-after-free — the simple case

CWE-416. The property we want to assert: _for every `free(x)`, no
execution path afterward may dereference `x` again_.

That's exactly the shape of `allExtended<Call>(...)` — a universal
quantifier over the matching nodes that returns a `QueryTree<Boolean>`
recording the per-call verdict and the evidence.

```kotlin
result.allExtended<Call>(sel = { it.name.localName == "free" }) { free ->
    val arg = free.arguments.first() as Reference
    not(executionPath(free, scope = Intraprocedural()) {
        it is Reference && it.refersTo == arg.refersTo && it != arg
    })
}
```

Read it out loud: _"for every call to `free`, assert that the freed
pointer is **not** reachable along any execution path afterward — within
the same function."_ No explicit imports, no `.filter` plumbing — the
query API does the heavy lifting and returns a tree we can navigate.

> `scope = Intraprocedural()` matters: pointer lifetime is bound to the
> declaring function, so an inter-procedural walk would cross into
> sibling callers (e.g. main → next function) and produce noise. The
> default scope is interprocedural; we narrow it for this query.

The REPL renders the tree: a root `✗` (the assertion failed overall),
with a child per `free` site. Two children are red `✗` (the violations
at `vuln.c:13` — simple UAF, and `vuln.c:31` — double free also shows
as UAF); the rest are green `✓`. Each violation expands further to show
the execution path that the analyzer walked to reach the use — the
evidence is right there.

Export the failing paths to VS Code:

```
:flow
```

The SARIF viewer opens with one finding per violation, each with the
full step-through path. _"This is the same workflow you'd get from a
mature SAST product — except the rule is a six-line Kotlin expression
you just wrote in the REPL."_

#### The production-grade variant

The EOG-based query above is great for the demo because it reads top to
bottom in one breath. In a real rule pack you'd lean on the
**PointsToPass** instead: every `free(p)` is modeled as a flow from an
`UnknownMemoryValue("taint.freed")` into `p`'s memory cell, and we ask
the DFG (not the EOG) whether that taint ever reaches a real use:

```kotlin
result.allExtended<de.fraunhofer.aisec.cpg.graph.declarations.Function>(
    sel = { it.name.localName == "free" },
    mustSatisfy = { fd ->
        fd.functionSummary.values.flatMap { it }
            .map { it.srcNode }
            .filterIsInstance<UnknownMemoryValue>()
            .filter { it.isTaint("freed") }
            .map { taint ->
                not(
                    dataFlow(
                        taint,
                        sensitivities = FieldSensitive + ContextSensitive + OnlyFullDFG,
                        predicate = { sink ->
                            sink is Reference ||
                            ((sink as? ParameterMemoryValue)?.let { pmv ->
                                pmv.name.toString().endsWith(".derefvalue") &&
                                !pmv.name.toString().startsWith("free.")
                            } == true)
                        }
                    )
                ).apply { if (!this.value) this.stringRepresentation = "CWE-416" }
            }
            .mergeWithAll()
    },
)
```

Two reasons this is the "production" version:

- It rides on **function summaries**, so libc operations are modeled
  declaratively from YAML (`cxx-stdlib-flows.yml`) instead of pattern-
  matching the call site. New unsafe-after-free wrappers (`zfree`,
  `safe_free`, …) just need a one-line summary, not a query change.
- `FieldSensitive + ContextSensitive + OnlyFullDFG` follows the value
  through struct fields and across call boundaries, but skips partial
  DFG edges — which keeps the path count tractable on real codebases.

The REPL flips ✓/✗ for everything underneath the `not(...)`, so the
audience reads each path with the safety-query polarity (✓ = no flow,
safe; ✗ = flow found, violation) instead of the dataflow-internal
polarity (✓ = path found). Each violating path lists its source-level
`calls:` chain so you can click straight to the offending `printf` or
`free` site.

Two demo-specific gotchas worth mentioning when you run it:

- `Function` has to be fully qualified
  (`de.fraunhofer.aisec.cpg.graph.declarations.Function`) — otherwise
  Kotlin resolves it to `kotlin.Function<R>` from the standard library
  and the lambdas can't infer their parameter types.
- The bundled stdlib summaries match languages by class-name suffix, so
  C and C++ need separate YAML entries. The repo ships both for `free`;
  if you add a new libc shim, duplicate it for `CLanguage` and
  `CPPLanguage`.

### Demo: Value Evaluation

The value evaluators compute numeric bounds and track data flow through
pointers. Two evaluators are available:

1. **Default evaluator** — returns a single value (first DFG path only)
2. **MultiValueEvaluator** — returns all possible values from multiple DFG paths

**Array size bounds:**

```kotlin
sizeBounds(result.functions["bounded_alloc"].variables["buf"])
```

Output: `[16, 64]` — the evaluator joined two malloc branches!

**Integer value tracking (single value):**

```kotlin
val func = result.functions["eval_chained_pointer"]!!
val cRef = func.calls[0].arguments.last()
cRef.evaluate()
```

Output: `99` — tracks pointer dereferences including chained `**pp`.

**Multiple values (all DFG paths):**

```kotlin
val func = result.functions["eval_struct_member"]!!
val printfCall = func.calls[1]
printfCall.arguments[1].evaluate(MultiValueEvaluator())
```

Output: `ConcreteNumberSet[[10, 99]]` — returns all values from all DFG
paths, useful when there are multiple assignments to the same location.

### Demo: PointsToPass and DFG Edges

The **PointsToPass** runs automatically and stitches DFG edges through
pointer indirection. The payoff: the value evaluators follow `*p`,
`pp->x`, and inter-procedural mutations _for free_.

**Inter-procedural pointer write:**

```c
void set3(int *p)        { *p = 3; }
void eval_inter_proc_pointer(void) {
    int x = 0;
    set3(&x);
    printf("x = %d\n", x);
}
```

```kotlin
val xRef = result.functions["eval_inter_proc_pointer"].calls[1].arguments.last()
xRef.evaluate()
```

Output: `3` — PointsToPass resolved `p` inside `set3` back to the
caller's `x`, and the evaluator propagated `*p = 3` across the call.

**Struct member access across a function boundary** — same machinery,
now for fields:

```c
static void set_point_x(struct Point *pp, int val) { pp->x = val; }

void eval_struct_member(void) {
    Point p;
    p.x = 10;
    p.y = 20;
    (*pp).y = 25;
    set_point_x(&p, 99);
    printf("x=%d y=%d\n", p.x, p.y);
}
```

```kotlin
val func = result.functions["eval_struct_member"]
val px = func.memberExpressions("x").last()
val py = func.memberExpressions("y").last()

interval(px)  // [10, 99]
interval(py)  // [20, 25]
```

`interval()` runs the `IntegerIntervalEvaluator` and returns the full
`[lower, upper]` bounds. `p.x` → `[10, 99]` merges the local `p.x = 10`
and the inter-procedural `set_point_x(&p, 99)`. `p.y` → `[20, 25]` merges
`p.y = 20` and `(*pp).y = 25`. Field-name filtering ensures `p.x` writes
don't leak into `p.y` reads.

### Buffer Overflows

CWE-120. The property: _for every `strcpy`, the destination must be
large enough to hold the source_. Risk = the destination has a known
small bound AND the source is unbounded (could be longer).

The CPG ships `sizeBounds()` — runs the abstract-value evaluator
(`ArraySizeEvaluator`) and returns the result as a
`QueryTree<LatticeInterval>` so the _bounds_, not just an upper
estimate, are preserved. The evaluator handles:

- fixed-size arrays (`char buf[8]` → `8`)
- string literals (`"secret"` → `6`)
- `InitializerList` and `ArrayConstruction` of known shape
- `malloc(constant)` calls (`char *p = malloc(64)` → `64`)
- everything else (parameters, opaque pointers) → `[-∞, ∞]` (TOP) or `⊥`

Use the `couldExceed` operator from the `LatticeInterval` API to
compare the two intervals directly — _"could the source's value
exceed the destination's?"_ — and thread both `sizeBounds` calls as
children so the evaluated intervals appear right in the rendered tree:

```kotlin
result.allExtended<Call>(sel = { it.name.localName == "strcpy" }) { call ->
    val ds = sizeBounds(call.arguments[0])
    val ss = sizeBounds(call.arguments[1])
    val unsafe = ss.value couldExceed ds.value
    QueryTree(
        value = !unsafe,
        children = listOf(ds, ss),
        stringRepresentation = if (unsafe) "overflow risk" else "ok",
        operator = GenericQueryOperators.EVALUATE,
        node = call,
    )
}
```

`couldExceed` (and its dual `fitsIn`) live on `LatticeInterval` and
handle the corner cases the way you'd want: a `[-∞, ∞]` source against
a `[8, 8]` dest is "could exceed" (the unbounded side wins), but
unknown vs unknown isn't (nothing is asserted).

## 17:00 – 22:00 — Same queries, different language

### Step 5 — Concepts and overlays

The CPG can attach _concepts_ — semantic overlays — to nodes. The Python
frontend ships with passes that auto-detect:

- **File operations** (`open`, `os.system`, etc.) → `OpenFile`, `WriteFile`, `ReadFile`
- **Logging** (`logging.info`, `logger.error`, etc.) → `LogWrite`, `Logging`
- **Configuration** (`configparser.ConfigParser`) → `Configuration`, `ConfigurationSource`

Let's load the demo with a concepts file that also tags a variable as `Secret`:

```
:reload examples/02-python-flow --concepts examples/02-python-flow/app.concepts.yaml
```

Now query the concepts:

```kotlin
// File operations (auto-tagged)
result.overlays<OpenFile>()
result.overlays<WriteFile>()

// Logging (auto-tagged)
result.overlays<LogWrite>()
result.overlays<Logging>()

// Configuration from config.ini (auto-tagged)
result.overlays<Configuration>()

// Secret (manually tagged via YAML)
result.overlays<Secret>()

// Security check: verify NO secrets flow to files
result.allExtended<Secret>(
    mustSatisfy = { secret ->
        not(
            dataFlow(
                startNode = secret,
                direction = Forward(GraphToFollow.DFG),
                scope = Interprocedural(),
                predicate = { it is WriteFile }
            )
        )
    }
)
```

The `Secret` overlay was loaded from `app.concepts.yaml` — it marks the
`MY_SECRET` variable. Let's follow where this secret flows:

```kotlin
val secret = result.overlays<Secret>().first()
secret.underlyingNode
```

The security query above returns `false` because `MY_SECRET` flows to a
`WriteFile` via `f.write(f"Secret: {MY_SECRET}\n")`. This is a data
leakage finding.

> **Talking point:** concepts are _layers_ on top of the raw AST. We can
> auto-detect common patterns (file I/O, logging, config) and let analysts
> add custom tags (like `Secret`) that then participate in the same data-flow
> queries as everything else.

---

## 22:00 – 28:00 — From query to policy: Codyze compliance

Quit the REPL (`:quit`) and switch hats: ad-hoc queries are great for
exploring, but for _governance_ you want named, signed requirements over
a structured project.

> **Project:** `examples/04-compliance/` — a tiny app with two components
> (`auth`, `webapp`) and one requirement: _"For each key K used in
> encryption/decryption, K must be deleted afterward."_

Look at `project.codyze.kts`:

```bash
sed -n '30,60p' examples/04-compliance/project.codyze.kts
```

The interesting block is `requirements { … fulfilledBy { properHandlingOfKeyMaterial() } … }`
— and below it, the Kotlin function `properHandlingOfKeyMaterial()` is
_the same shape_ as the REPL queries we just wrote: it uses
`allExtended<Call>`, `executionPath`, and the `Delete` node type.

Run the analyzer:

```bash
codyze compliance scan --project-dir=examples/04-compliance --console=true
```

The `--console=true` flag opens the web console after the scan so you can
explore the results interactively.

Or for the REPL (analyze a component's source directly):

```bash
codyze repl examples/04-compliance/components/auth/auth
```

You get a SARIF report with the requirement marked fulfilled. Now break
the property:

```bash
# Edit components/webapp/webapp/main.py and delete the `del my_secret` line.
sed -i.bak '/del my_secret/d' examples/04-compliance/components/webapp/webapp/main.py

codyze compliance scan --project examples/04-compliance
```

The same requirement is now reported as failing, with the offending call
site linked. **Restore:**

```bash
mv examples/04-compliance/components/webapp/webapp/main.py.bak \
   examples/04-compliance/components/webapp/webapp/main.py
```

> **Talking point:** the REPL is for exploration; the compliance DSL is
> for accountability. The same query API powers both — the requirement
> in `project.codyze.kts` _is_ the kind of query you just iterated on
> in the REPL, lifted into a signed, named, repeatable check.

---

## 28:00 – 30:00 — Open exploration

Take audience suggestions. Useful starting points:

```kotlin
// Re-open the REPL on the compliance project for free-form play.
:reload examples/04-compliance
```

```kotlin
// "Show me everything that flows into os.system."
result.calls
    .filter { it.name.localName == "execute" }
    .map { it.arguments.firstOrNull() }
```

```kotlin
// "Save my session so I can clean it up later."
:save /tmp/demo-session.cpg.query.kts
```

```kotlin
// "What concepts (overlays) do we have on this graph?"
result.nodes.flatMap { it.overlays }.distinctBy { it::class }
```

---

## Cheat-sheet

| You want to …             | REPL one-liner                                     |
| ------------------------- | -------------------------------------------------- |
| List all functions        | `result.functions`                                 |
| Find calls to X           | `result.calls.filter { it.name.localName == "X" }` |
| Follow a value forward    | `dataFlow(startNode = node) { it == sink }`        |
| Follow execution forward  | `executionPath(startNode = node) { it is …Call }`  |
| Get all `Reference` nodes | `result.refs`                                      |
| Re-analyze new code       | `:reload <path>`                                   |
| Save session as script    | `:save <file>`                                     |

| Meta-command     | Effect                                            |
| ---------------- | ------------------------------------------------- |
| `:help`          | show all commands                                 |
| `:reload [path]` | re-run analysis (with or without new source)      |
| `:imports`       | list auto-imported packages                       |
| `:result`        | one-line summary of the current TranslationResult |
| `:save <file>`   | dump evaluated lines to a `.cpg.query.kts`        |
| `:quit`          | exit (also Ctrl-D)                                |

| What you see in a hyperlink | What you click to                                                   |
| --------------------------- | ------------------------------------------------------------------- |
| `vuln.c:24`                 | `file://…/vuln.c#L24` — opens at that line in the OS-default editor |

---

## Known caveats (so you don't get surprised on stage)

1. **First eval is slow.** The Kotlin scripting compiler warms up on the
   first snippet (~2–4 s on a modern laptop). Subsequent snippets are
   sub-second. Run _one warm-up query_ before the audience walks in.
2. **`sun.misc.Unsafe` warning.** The Kotlin compiler prints a JVM
   deprecation warning to stderr on each eval. Cosmetic; harmless.
   Silence by adding `--add-opens=java.base/java.lang=ALL-UNNAMED` to the
   `codyze` launch script if you want a clean stage.
3. **Alias-aware UAF.** The naive `refersTo == refersTo` predicate misses
   the `uaf_aliased` case. Don't claim it found it — use it as the bridge
   to the points-to discussion (which is genuinely on the roadmap).
4. **Web frontend (`pnpmInstall`)** is currently broken in this checkout.
   We skip those Gradle tasks. The REPL is unaffected.
