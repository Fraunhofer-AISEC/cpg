# Query Examples

We want to create a way to create "rules" or "checks" that check for certain patterns in the code. Therefore, we need to decide if we want to have an "algorithmic" or a "descriptive" way to declare such a check.

Syntax explanation: `|x|` means that `x` should be "resolved", either through constant propagation or other fancy algorithms.

The following examples check that no such bug is present.

## Array out of bounds exception

Part of: CWE 119
```
result.all<ArraySubscriptionExpression>(mustSatisfy = { max(it.subscriptExpression) < min(it.size) && min(it.subscriptExpression) >= 0 })
```

## Null pointer dereference (CWE 476)

```
result.all<HasBase>(mustSatisfy={it.base() != null})
```

## Memcpy too large source (Buffer Overflow)
Part of CWE 120: Buffer Copy without Checking Size of Input ('Classic Buffer Overflow') --> do we also need to find self-written copy functions for buffers?
```
result.all<Call>({ it.name == "memcpy" }, { sizeof(it.arguments[0]) >= min(it.arguments[2]) } )
```

## Memcpy too small source

```
result.all<Call>({ it.name == "memcpy" }, { sizeof(it.arguments[1]) <= max(it.arguments[2]) } )
```

## Division by 0 (CWE 369)

```
result.all<BinaryOperator>({ it.operatorCode == "/" }, { !(it.rhs.evaluate(MultiValueEvaluator()) as NumberSet).maybe(0) } )
```

## Integer Overflow/Underflow (CWE 190, 191, 128)

For assignments:
```
result.all<Assignment>({ it.target?.type?.isPrimitive == true }, { max(it.value) <= maxSizeOfType(it.target!!.type) && min(it.value) >= minSizeOfType(it.target!!.type)})
```
For other expressions, we need to compute the effect of the operator.

## Use after free

Intuition: No node which is reachable from a node `free(x)` must use `x`. Use EOG for reachability, but I'm not sure how to say "don't use x". This is the most basic form.
```
result.all<Call>({ it.name == "free" }) { outer -> !executionPath(outer) { (it as? DeclaredReferenceExpression)?.refersTo == (outer.arguments[0] as? DeclaredReferenceExpression)?.refersTo }.value }
```

## Double Free

```
result.all<Call>({ it.name == "free" }) { outer -> !executionPath(outer) { ((it as? Call)?.name == "free" && ((it as? Call)?.arguments?.getOrNull(0) as? DeclaredReferenceExpression)?.refersTo == (outer.arguments[0] as? DeclaredReferenceExpression)?.refersTo }.value }
```

## Format string attack

arg0 of functions such as `printf` must not be user input. Since I'm not aware that we have a general model for "this is user input" (yet), we could say that all options for the argument must be a Literal (not sure if the proposed notation makes sense though).
```
vuln_fcs = ["fprint", "printf", "sprintf", "snprintf", "vfprintf", "vprintf", "vsprintf", "vsnprintf"];
forall (n: Call): n.invokes.name in vuln_fcs => forall u in |backwards_DFG(n.arguments[0])|: u is Literal
```

Since many classical vulns. (injection) are related to user input, we probably need a way to specify sources of user input (or "sources" in general). To reduce FP, we probably also want to check some conditions over the path between the source and the sink (e.g. some checks are in place to check for critical characters/substrings, do escaping, etc.). Problem: There are tons of options.

## Access of Uninitialized Pointer (CWE 824)

## Access of Invalid Memory Address

## Unsecure Default Return Value

Sounds like this always depends on the program? What is an insecure return value?

E.g.:
* Authorization: instead of assuming successful authorization (`authorized = true`) and checking for the contrary; start with assuming unauthorized (`authorized = false`) and check for authorization.

## Missing Return Value Validation (Error checking)

CWE 252

I can't think of a simple query here which does not introduce too many findings because it often depends on "what happens afterward". Example: logging an error value is typically not problematic. Also, the return values can have very different meanings which makes it hard to find a solution for all issues.

Simple idea 1: There has to be at least a check for the return value (probably for a given list of functions and respective error indicating return values).

## Command Injection

* Perform data flow analysis and check if unchecked user input reaches function calling system commands.

## Proper Nulltermination of Strings (C specific)

## Improper Certificate Validation (CWE 306)

=> Use codyze?

## Use of Hard-coded Credentials (CWE 798)

Idea: when crypto API is known, we could follow the input argument for passwords/keys ...

```
relevant_args = {"function": "arg0"}
forall (n: Call): n.invokes.name in relevant_args.keys => forall u in |backwards_DFG(relevant_args(n.invokes.name))|: u !is Literal
```

## Scribbles

### Test arguments of call expression
```
result.all<Call>({ it.name == "<function name>" }) { it.arguments[<no.>].value!! == const(<value>) }
```

### Track return value of call expression

```
forall (n1: Call, n2: Call): n1.invokes.name == "<function name 1>" && n2.invokes.name == "<function name 2>" => data_flow(n1.returnValue, n2.arguments[<no.>])
```

### Ensure path property
```
forall (n: Call, v: Value) : n.invokes.name == "<function name>" && data_flow(v, n.arguments[<no.>]) => inferred_property(v, <property>)
```

Example:
```
val algo = read_from_file(/* some file */);
if (val != "AES") {
  throw Exception();
}
val cipher = initialize_cipher(algo); // at this point one can infer that algo must have the value "AES"
```

## https://cwe.mitre.org/data/definitions/1228.html

Should be easy by simply maintaining a list of the dangerous, inconsistent, obsolete, etc. functions and checking all `Call`s.

# Which analyses do we need?

* Integer range
* Buffer size of constant sized arrays (mem size, no elements)
* Data flow analysis (intraproc: DFG edges, interproc: accessible via `followDFGEdgesUntilHit`)
* Reachability (intraproc: EOG edges, interproc: accessible via `followEOGEdgesUntilHit`)
* Points-to information
* Taint analysis
* Constant propagation