---
title: "Graph Traversal – Examples"
linkTitle: "Examples"
weight: 27
description: >
    Annotated end-to-end examples showing how to analyse C programs with the followXXX functions.
---

# Graph Traversal Examples

Each example below shows a small C program and the Kotlin analysis code that queries the CPG of
that program. The examples progressively cover the most common use cases: backward DFG analysis,
intraprocedural scope, `earlyTermination`, EOG reachability, CDG, and collecting all paths.

All snippets assume:

```kotlin
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
```

---

## Example 1 – Backward DFG: does an argument always come from a literal?

**Goal:** Detect calls to `encrypt()` where the key argument might not be a hard-coded string
literal. If any DFG path from the argument leads back to something other than a `Literal`, the
key is potentially attacker-controlled.

=== "C source"

    ```c title="encrypt_check.c"
    #include <string.h>

    /* Encrypts data using `key`. The key should always be a known constant. */
    void encrypt(const char *data, const char *key);

    /* Safe: key is always a string literal */
    void process_safe(const char *data) {
        encrypt(data, "s3cr3t_k3y");
    }

    /* Unsafe: key comes from a function parameter (potentially attacker-controlled) */
    void process_unsafe(const char *data, const char *user_key) {
        encrypt(data, user_key);
    }
    ```

=== "Kotlin analysis"

    ```kotlin
    for (call in result.calls["encrypt"]) {
        // The second argument (index 1) is the key
        val keyArg = call.arguments[1]

        // Walk the DFG backwards from the key argument to find its ultimate source(s).
        // A path is "fulfilled" if it reaches a Literal node.
        // A path is "failed" if it ends at something else (e.g. a parameter).
        val (fulfilled, failed) = keyArg.followDFGEdgesUntilHit(
            direction = Backward(GraphToFollow.DFG),
            sensitivities = FieldSensitive + ContextSensitive,
            scope = Interprocedural(),
        ) { node -> node is Literal<*> }

        if (failed.isEmpty()) {
            println("${call.location}: key ALWAYS originates from a literal ✓")
        } else {
            println("${call.location}: key can come from a non-literal source ✗")
            for ((reason, path) in failed) {
                println("  Reason: $reason, last node: ${path.nodes.last()}")
            }
        }
    }
    ```

**Expected output:**

- `process_safe`: `fulfilled` is non-empty and `failed` is empty → key is always a literal ✓  
- `process_unsafe`: `failed` contains a path ending at the `user_key` parameter → key is NOT
  always a literal ✗

---

## Example 2 – Intraprocedural DFG: does tainted data reach a sensitive sink?

**Goal:** Check whether user-supplied input (`data`) reaches a call to `log_message()` *within the
same function body*, without crossing any function boundaries.

=== "C source"

    ```c title="logging_check.c"
    #include <string.h>

    void sanitize(char *buf, int len);
    void log_message(const char *msg);

    /* Safe: data is sanitized before logging */
    void handle_request_safe(char *data, int len) {
        sanitize(data, len);
        log_message(data);   /* data was sanitized first */
    }

    /* Unsafe: data flows directly into log_message without sanitization */
    void handle_request_unsafe(char *data, int len) {
        log_message(data);   /* no sanitize() call before this */
        sanitize(data, len);
    }
    ```

=== "Kotlin analysis"

    ```kotlin
    for (call in result.calls["log_message"]) {
        val logArg = call.arguments[0]

        // Walk the DFG backwards from the log_message argument,
        // staying within the current function (Intraprocedural scope).
        // We look for a preceding call to sanitize() on the same data.
        val (fulfilled, failed) = logArg.followDFGEdgesUntilHit(
            direction = Backward(GraphToFollow.DFG),
            scope = Intraprocedural(),      // stay in this function only
            collectFailedPaths = false,     // we only need to know if any path is safe
        ) { node ->
            // The predicate is satisfied when we reach the argument of a sanitize() call
            node is CallExpression && node.name.localName == "sanitize"
        }

        if (fulfilled.isNotEmpty()) {
            println("${call.location}: data was sanitized before logging ✓")
        } else {
            println("${call.location}: data reaches log_message WITHOUT sanitization ✗")
        }
    }
    ```

**Expected output:**

- `handle_request_safe` → fulfilled path found → data is sanitized ✓  
- `handle_request_unsafe` → no fulfilled path → data reaches `log_message` unsanitized ✗

---

## Example 3 – `earlyTermination`: stop at function borders

**Goal:** Follow the DFG forward from a `malloc()` call result, but stop the traversal the
moment it would enter a different function body. This is useful when you want to keep the analysis
*almost* intraprocedural but still follow through helper macros or inline code.

=== "C source"

    ```c title="malloc_check.c"
    #include <stdlib.h>
    #include <string.h>

    /* Checks that the result of malloc() is always used within the same function
     * and not passed blindly to another function without a NULL check first. */

    void use_buffer(char *buf);

    void safe_usage() {
        char *buf = (char *)malloc(64);
        if (buf == NULL) return;   /* null check before use */
        memset(buf, 0, 64);
        use_buffer(buf);
        free(buf);
    }

    void unsafe_usage() {
        char *buf = (char *)malloc(64);
        use_buffer(buf);   /* no null check! */
        free(buf);
    }
    ```

=== "Kotlin analysis"

    ```kotlin
    for (call in result.calls["malloc"]) {
        // Follow DFG forward from the malloc call result.
        // Stop if the path would enter a FunctionDeclaration (i.e. cross a function border).
        val (fulfilled, failed) = call.followDFGEdgesUntilHit(
            scope = Interprocedural(),
            earlyTermination = { nextNode, _ -> nextNode is FunctionDeclaration },
        ) { node ->
            // Target: a null check on the pointer (BinaryOperator comparing against 0/NULL)
            node is BinaryOperator &&
                (node.operatorCode == "==" || node.operatorCode == "!=") &&
                node.rhs is Literal<*> && (node.rhs as Literal<*>).value == 0
        }

        val crossedBorder = failed.any { it.first == FailureReason.HIT_EARLY_TERMINATION }
        if (fulfilled.isNotEmpty()) {
            println("${call.location}: malloc result is null-checked before use ✓")
        } else if (crossedBorder) {
            println("${call.location}: malloc result was passed to another function without a null check")
        } else {
            println("${call.location}: malloc result is used WITHOUT a null check ✗")
        }
    }
    ```

---

## Example 4 – `scope` with `maxCallDepth`: limit interprocedural depth

**Goal:** Verify that a sensitive value (e.g. a password read from the environment) does not
flow into a `printf()`-like logging function, following at most 2 call levels deep.

=== "C source"

    ```c title="password_leak.c"
    #include <stdio.h>
    #include <stdlib.h>

    void debug_print(const char *msg) {
        printf("[DEBUG] %s\n", msg);   /* leaks msg to stdout */
    }

    void log_info(const char *msg) {
        debug_print(msg);  /* call depth 2 from the source */
    }

    void authenticate() {
        const char *password = getenv("APP_PASSWORD");
        log_info(password);  /* password leaks via 2 call levels */
    }
    ```

=== "Kotlin analysis"

    ```kotlin
    for (call in result.calls["getenv"]) {
        // Follow the DFG forward from the getenv() result, up to 2 call levels deep.
        val (fulfilled, _) = call.followDFGEdgesUntilHit(
            scope = Interprocedural(maxCallDepth = 2),
        ) { node ->
            // Target: any argument position of a printf-like call
            node is CallExpression &&
                node.name.localName in listOf("printf", "fprintf", "puts", "fputs")
        }

        if (fulfilled.isNotEmpty()) {
            println("${call.location}: sensitive value from getenv() reaches a print function ✗")
        } else {
            println("${call.location}: no direct leak to a print function detected ✓")
        }
    }
    ```

---

## Example 5 – Forward EOG: does execution always pass through a null check?

**Goal:** Ensure that every execution path through `deref_pointer()` passes through the null check
before dereferencing the pointer. If any path *skips* the null check, the function may crash.

=== "C source"

    ```c title="null_check.c"
    #include <stdio.h>

    /* Safe: null check is always performed before dereferencing */
    int safe_deref(int *ptr) {
        if (ptr == NULL) {
            return -1;
        }
        return *ptr;
    }

    /* Unsafe: dereference happens before the null check on one branch */
    int unsafe_deref(int *ptr) {
        int val = *ptr;        /* possible NULL dereference! */
        if (ptr == NULL) {
            return -1;
        }
        return val;
    }
    ```

=== "Kotlin analysis"

    ```kotlin
    for (fn in result.functions["safe_deref", "unsafe_deref"]) {
        val startNode = fn.body ?: continue

        // Walk the EOG forward from the start of the function body,
        // staying within the function (Intraprocedural).
        // The target is a UnaryOperator dereference (*ptr).
        val derefNodes = fn.allChildren<UnaryOperator> { it.operatorCode == "*" }
        if (derefNodes.isEmpty()) continue

        val derefNode = derefNodes.first()

        // Check if there is an EOG path from the function entry to the dereference
        // that does NOT pass through a null check (i.e. a BinaryOperator "== NULL").
        val (toDeref, _) = startNode.followEOGEdgesUntilHit(
            direction = Forward(GraphToFollow.EOG),
            scope = Intraprocedural(),
        ) { it == derefNode }

        // For each path that reaches the dereference, walk backwards to see
        // if a null check was encountered.
        var allPathsProtected = true
        for (path in toDeref) {
            val hasNullCheck = path.nodes.any { node ->
                node is BinaryOperator &&
                    (node.operatorCode == "==" || node.operatorCode == "!=") &&
                    node.operands.any { it is Literal<*> && it.value == 0 }
            }
            if (!hasNullCheck) {
                allPathsProtected = false
            }
        }

        if (allPathsProtected) {
            println("${fn.name}: all paths to dereference are null-checked ✓")
        } else {
            println("${fn.name}: some paths reach dereference WITHOUT a null check ✗")
        }
    }
    ```

---

## Example 6 – Backward CDG: which conditions control a sensitive operation?

**Goal:** Find all `if`-conditions that control whether a `write_to_file()` call is executed. This
is useful to verify that sensitive file writes are always guarded by an authorisation check.

=== "C source"

    ```c title="access_control.c"
    #include <stdio.h>

    void write_to_file(const char *filename, const char *data);

    void process_admin_request(int is_admin, const char *data) {
        if (is_admin) {
            /* This write is controlled by the is_admin check */
            write_to_file("/etc/app.conf", data);
        }
    }

    void process_any_request(const char *data) {
        /* No access control – write is always executed */
        write_to_file("/tmp/output.txt", data);
    }
    ```

=== "Kotlin analysis"

    ```kotlin
    for (call in result.calls["write_to_file"]) {
        // Walk the CDG backwards from the write_to_file() call
        // to find all IfStatement conditions that control it.
        val (controllingIfs, _) = call.followPrevCDGUntilHit(
            interproceduralAnalysis = false,
        ) { it is IfStatement }

        if (controllingIfs.isNotEmpty()) {
            println("${call.location}: write is guarded by an if-statement ✓")
            for (path in controllingIfs) {
                val ifNode = path.nodes.last() as IfStatement
                println("  Condition: ${ifNode.condition}")
            }
        } else {
            println("${call.location}: write has NO controlling if-statement ✗")
        }
    }
    ```

**Expected output:**

- `process_admin_request` → `write_to_file` is guarded by `if (is_admin)` ✓  
- `process_any_request` → `write_to_file` has no controlling `if` ✗

---

## Example 7 – Collecting all reachable DFG paths for inspection

**Goal:** Enumerate every DFG path that data can take from the return value of `fgets()` to
understand where user input can propagate in the program.

=== "C source"

    ```c title="user_input.c"
    #include <stdio.h>
    #include <string.h>

    void process(char *buf);
    void log_it(const char *msg);

    void read_and_handle() {
        char buf[256];
        fgets(buf, sizeof(buf), stdin);  /* user input enters here */
        process(buf);
        log_it(buf);
    }
    ```

=== "Kotlin analysis"

    ```kotlin
    for (call in result.calls["fgets"]) {
        // The first argument of fgets() is the buffer that receives user input.
        val bufArg = call.arguments[0]

        // Collect every DFG path originating from the buffer argument.
        val allPaths: List<NodePath> = bufArg.collectAllNextDFGPaths()

        println("${call.location}: ${allPaths.size} DFG path(s) from fgets buffer:")
        for (path in allPaths) {
            val last = path.nodes.last()
            println("  → ${path.nodes.joinToString(" → ") { it.javaClass.simpleName }}")
            println("    last node: $last")
        }
    }
    ```
