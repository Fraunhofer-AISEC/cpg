/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified.Wire.Sexp
import CpgVerified.Go.Translate

/-!
# Encoding of Go ASTs and CPG nodes

Go expressions arrive as records

```
(expr NAMESCOPE PACKAGES IOTA SHADOWED EXPR)
NAMESCOPE := () | (name)
PACKAGES  := (name*)
IOTA      := () | (int)
SHADOWED  := (name*)
EXPR      := (lit START END KIND VALUE)           KIND := int | float | imag | char | string
           | (ident START END NAME)
           | (binary START END OP X Y)            OP is the operator token, e.g. +
           | (unary START END OP X)
           | (paren START END X)
           | (call START END FN (ARG*))
           | (selector START END X SEL)
           | (index START END X INDEX)
           | (slice START END X OPT OPT OPT)      OPT := () | (EXPR)
           | (star START END X)
           | (typeassert START END X OPT)         OPT := () | (TYPEEXPR)
           | (compositelit START END OPT (ELT*))   OPT := () | (TYPEEXPR)
           | (keyvalue START END KEY VALUE)
           | (arraytype START END ELT)
           | (maptype START END KEY VALUE)
           | (chantype START END VALUE)
           | (unsupported START END GOTYPE)
```

and translated CPG expressions are sent back as

```
CPG := (literal START END VALUE TYPE NAME)
         VALUE := (int n) | (bool true|false) | (str s) | (nil) | (obj address) | (float text)
         TYPE  := (primitive name) | (unknown) | (object name (TYPE*)) | (pointer TYPE)
                | (array TYPE) | (resolved TYPE)
         NAME  := () | (name)
     | (reference START END NAME)
     | (binary START END CODE LHS RHS)
     | (unary START END CODE INPUT)
     | (call START END CALLEE (ARG*))
     | (member START END NAME BASE)
     | (membercall START END CALLEE (ARG*))
     | (subscription START END ARRAY SUBSCRIPT)
     | (range START END OPT OPT OPT)           OPT := () | (CPG)
     | (deref START END INPUT)
     | (cast START END TYPE EXPRESSION)
     | (new START END TYPE INITIALIZER)
     | (initializerlist START END TYPE (INITIALIZER*))
     | (keyvalue START END KEY VALUE)
     | (construction START END TYPE (ARG*))
     | (arrayconstruction START END TYPE (DIMENSION*))
     | (problem START END MESSAGE)
```
-/

namespace Wire

open Cpg (Span Value TypeRef)

def BinaryOp.all : List Go.BinaryOp :=
  [.add, .sub, .mul, .quo, .rem, .and, .or, .xor, .shl, .shr, .andNot, .land, .lor, .eql, .neq,
   .lss, .leq, .gtr, .geq]

def UnaryOp.all : List Go.UnaryOp := [.add, .sub, .not, .xor, .arrow, .and, .tilde]

def decodeNat : Sexp → Except String Nat
  | .atom s => match s.toNat? with
    | some n => pure n
    | none => throw s!"expected a natural number, got {s}"
  | s => throw s!"expected a natural number, got {repr s}"

def decodeInt : Sexp → Except String Int
  | .atom s => match s.toInt? with
    | some n => pure n
    | none => throw s!"expected an integer, got {s}"
  | s => throw s!"expected an integer, got {repr s}"

def decodeString : Sexp → Except String String
  | .atom s => pure s
  | s => throw s!"expected an atom, got {repr s}"

def decodeSpan (start stop : Sexp) : Except String Span :=
  return ⟨← decodeNat start, ← decodeNat stop⟩

def decodeLitKind : String → Except String Go.LitKind
  | "int" => pure .int
  | "float" => pure .float
  | "imag" => pure .imag
  | "char" => pure .char
  | "string" => pure .string
  | k => throw s!"unknown literal kind {k}"

mutual

partial def decodeOptExpr : Sexp → Except String (Option Go.Expr)
  | .list [] => pure none
  | .list [x] => some <$> decodeExpr x
  | s => throw s!"expected an optional expression, got {repr s}"

partial def decodeExpr : Sexp → Except String Go.Expr
  | .list [.atom "lit", s, e, .atom kind, .atom value] =>
    return .basicLit (← decodeSpan s e) (← decodeLitKind kind) value
  | .list [.atom "ident", s, e, .atom name] => return .ident (← decodeSpan s e) name
  | .list [.atom "binary", s, e, .atom op, x, y] => do
    let some op := BinaryOp.all.find? (·.token == op) | throw s!"unknown binary operator {op}"
    return .binary (← decodeSpan s e) (← decodeExpr x) op (← decodeExpr y)
  | .list [.atom "unary", s, e, .atom op, x] => do
    let some op := UnaryOp.all.find? (·.token == op) | throw s!"unknown unary operator {op}"
    return .unary (← decodeSpan s e) op (← decodeExpr x)
  | .list [.atom "paren", s, e, x] => return .paren (← decodeSpan s e) (← decodeExpr x)
  | .list [.atom "call", s, e, fn, .list args] =>
    return .call (← decodeSpan s e) (← decodeExpr fn) (← args.mapM decodeExpr)
  | .list [.atom "selector", s, e, x, .atom sel] =>
    return .selector (← decodeSpan s e) (← decodeExpr x) sel
  | .list [.atom "index", s, e, x, i] =>
    return .index (← decodeSpan s e) (← decodeExpr x) (← decodeExpr i)
  | .list [.atom "slice", s, e, x, low, high, max] =>
    return .slice (← decodeSpan s e) (← decodeExpr x) (← decodeOptExpr low)
      (← decodeOptExpr high) (← decodeOptExpr max)
  | .list [.atom "star", s, e, x] => return .star (← decodeSpan s e) (← decodeExpr x)
  | .list [.atom "typeassert", s, e, x, type] =>
    return .typeAssert (← decodeSpan s e) (← decodeExpr x) (← decodeOptExpr type)
  | .list [.atom "compositelit", s, e, type, .list elts] =>
    return .compositeLit (← decodeSpan s e) (← decodeOptExpr type) (← elts.mapM decodeExpr)
  | .list [.atom "keyvalue", s, e, k, v] =>
    return .keyValue (← decodeSpan s e) (← decodeExpr k) (← decodeExpr v)
  | .list [.atom "arraytype", s, e, elt] => return .arrayType (← decodeSpan s e) (← decodeExpr elt)
  | .list [.atom "maptype", s, e, k, v] =>
    return .mapType (← decodeSpan s e) (← decodeExpr k) (← decodeExpr v)
  | .list [.atom "chantype", s, e, v] => return .chanType (← decodeSpan s e) (← decodeExpr v)
  | .list [.atom "unsupported", s, e, .atom goType] =>
    return .unsupported (← decodeSpan s e) goType
  | s => throw s!"malformed expression {repr s}"

end

def decodeOption (f : Sexp → Except String α) : Sexp → Except String (Option α)
  | .list [] => pure none
  | .list [x] => some <$> f x
  | s => throw s!"expected an optional value, got {repr s}"

/-- Decodes a translation request into the context and the expression. -/
def decodeRecord : Sexp → Except String (Go.Ctx × Go.Expr)
  | .list [.atom "expr", ns, .list pkgs, iota, .list shadowed, e] => do
    let ctx : Go.Ctx := {
      nameScope := ← decodeOption decodeString ns
      packages := ← pkgs.mapM decodeString
      iota := ← decodeOption decodeInt iota
      shadowed := ← shadowed.mapM decodeString }
    return (ctx, ← decodeExpr e)
  | s => throw s!"malformed record {repr s}"

def encodeSpan (span : Span) : List Sexp := [.atom (toString span.start), .atom (toString span.stop)]

def encodeValue : Value → Sexp
  | .int i => .list [.atom "int", .atom (toString i)]
  | .bool b => .list [.atom "bool", .atom (toString b)]
  | .str s => .list [.atom "str", .atom s]
  | .nil => .list [.atom "nil"]
  | .obj address => .list [.atom "obj", .atom (toString address)]
  | .float text => .list [.atom "float", .atom text]

partial def encodeType : TypeRef → Sexp
  | .primitive name => .list [.atom "primitive", .atom name]
  | .unknown => .list [.atom "unknown"]
  | .object name generics => .list [.atom "object", .atom name, .list (generics.map encodeType)]
  | .pointer t => .list [.atom "pointer", encodeType t]
  | .array t => .list [.atom "array", encodeType t]
  | .resolved t => .list [.atom "resolved", encodeType t]

partial def encodeExpr : Cpg.Expr → Sexp
  | .literal loc v t name =>
    .list ([.atom "literal"] ++ encodeSpan loc ++
      [encodeValue v, encodeType t, .list (name.toList.map .atom)])
  | .reference loc name => .list ([.atom "reference"] ++ encodeSpan loc ++ [.atom name])
  | .binaryOperator loc code l r =>
    .list ([.atom "binary"] ++ encodeSpan loc ++ [.atom code, encodeExpr l, encodeExpr r])
  | .unaryOperator loc code x =>
    .list ([.atom "unary"] ++ encodeSpan loc ++ [.atom code, encodeExpr x])
  | .call loc callee args =>
    .list ([.atom "call"] ++ encodeSpan loc ++ [encodeExpr callee, .list (args.map encodeExpr)])
  | .memberAccess loc name base =>
    .list ([.atom "member"] ++ encodeSpan loc ++ [.atom name, encodeExpr base])
  | .memberCall loc callee args =>
    .list ([.atom "membercall"] ++ encodeSpan loc ++
      [encodeExpr callee, .list (args.map encodeExpr)])
  | .subscription loc arr idx =>
    .list ([.atom "subscription"] ++ encodeSpan loc ++ [encodeExpr arr, encodeExpr idx])
  | .range loc floor ceiling third =>
    .list ([.atom "range"] ++ encodeSpan loc ++
      [floor, ceiling, third].map fun e => .list (e.toList.map encodeExpr))
  | .pointerDereference loc input =>
    .list ([.atom "deref"] ++ encodeSpan loc ++ [encodeExpr input])
  | .cast loc t e => .list ([.atom "cast"] ++ encodeSpan loc ++ [encodeType t, encodeExpr e])
  | .new loc t init => .list ([.atom "new"] ++ encodeSpan loc ++ [encodeType t, encodeExpr init])
  | .initializerList loc t inits =>
    .list ([.atom "initializerlist"] ++ encodeSpan loc ++
      [encodeType t, .list (inits.map encodeExpr)])
  | .keyValue loc k v => .list ([.atom "keyvalue"] ++ encodeSpan loc ++ [encodeExpr k, encodeExpr v])
  | .construction loc t args =>
    .list ([.atom "construction"] ++ encodeSpan loc ++ [encodeType t, .list (args.map encodeExpr)])
  | .arrayConstruction loc t dims =>
    .list ([.atom "arrayconstruction"] ++ encodeSpan loc ++
      [encodeType t, .list (dims.map encodeExpr)])
  | .problem loc msg => .list ([.atom "problem"] ++ encodeSpan loc ++ [.atom msg])

/-- Handles one request: decodes, translates and encodes the result (or an error). -/
def handleRecord (record : Sexp) : Sexp :=
  match decodeRecord record with
  | .ok (ctx, e) => encodeExpr (Go.translate ctx e)
  | .error msg => .list [.atom "error", .atom msg]

end Wire
