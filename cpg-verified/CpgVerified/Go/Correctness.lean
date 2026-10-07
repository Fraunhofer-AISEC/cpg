/-
Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
Licensed under the Apache License, Version 2.0.
-/
import CpgVerified.Go.Translate
import CpgVerified.Go.Semantics
import CpgVerified.Cpg.Semantics

/-!
# Correctness of the Go expression translation

The main result, `translate_correct`, states that evaluating the CPG produced by `translate`
gives exactly the value that the Go semantics gives the original expression, including
failures (run-time errors, unbound names, constructs outside the subset).

It requires that the Go environment and the CPG environment agree up to name resolution, and
that the predeclared identifiers which the translation treats specially are not shadowed.
-/

namespace Go

open Cpg (Value)

/-- The meaning of language-dependent CPG operators in Go. -/
def semantics : Cpg.LanguageSemantics where
  intQuo := Cpg.IntOps.quo
  intRem := Cpg.IntOps.rem

/-- The Go environment and the CPG environment describe the same state. -/
structure EnvAgree (ctx : Ctx) (genv : Env) (cenv : Cpg.Env) : Prop where
  /-- A reference to `resolveName ctx name` refers to the Go variable `name`. -/
  vars : ∀ name, cenv.vars (resolveName ctx name) = genv.vars name
  /-- A call of `resolveName ctx name` calls the Go function `name`. -/
  funcs : ∀ name, cenv.funcs (resolveName ctx name) = genv.funcs name
  /-- Predeclared constants that the context does not mark as shadowed are not declared. -/
  constsUnshadowed : ∀ name ∈ ["true", "false", "nil", "iota"], name ∉ ctx.shadowed →
    genv.vars name = none ∧ genv.funcs name = none
  /-- Built-in allocation functions that the context does not mark as shadowed are not declared. -/
  allocUnshadowed : ∀ name ∈ ["new", "make"], name ∉ ctx.shadowed → genv.funcs name = none
  /-- A shadowed predeclared identifier refers to its declaration, never to the predeclared value. -/
  shadowedDeclared : ∀ name ∈ ctx.shadowed, genv.vars name = none → predeclared ctx.iota name = none
  /-- Members of imported packages have the same qualified name in both environments. -/
  qualifiedVars : ∀ p ∈ ctx.packages, ∀ sel,
    cenv.vars (p ++ "." ++ sel) = genv.vars (p ++ "." ++ sel)
  /-- Functions of imported packages have the same qualified name in both environments. -/
  qualifiedFuncs : ∀ p ∈ ctx.packages, ∀ sel,
    cenv.funcs (p ++ "." ++ sel) = genv.funcs (p ++ "." ++ sel)
  /-- Both environments have the same heap. -/
  fields : cenv.fields = genv.fields
  /-- Both environments have the same methods. -/
  methods : cenv.methods = genv.methods
  /-- Both environments index, slice and dereference in the same way. -/
  index : cenv.index = genv.index
  slice : cenv.slice = genv.slice
  deref : cenv.deref = genv.deref

/-- Every strict Go binary operator means the same as the CPG operator code it is mapped to. -/
theorem evalBinaryOp_token (op : BinaryOp) (hand : op ≠ .land) (hor : op ≠ .lor) (a b : Value) :
    Cpg.evalBinaryOp semantics op.token a b = evalBinary op a b := by
  cases op <;> cases a <;> cases b <;> first | contradiction | rfl

/-- Every Go unary operator means the same as the CPG operator code it is mapped to. -/
theorem evalUnaryOp_token (op : UnaryOp) (a : Value) :
    Cpg.evalUnaryOp op.token a = evalUnary op a := by
  cases op <;> cases a <;> rfl

theorem token_eq_land (op : BinaryOp) : op.token = "&&" ↔ op = .land := by
  cases op <;> decide

theorem token_eq_lor (op : BinaryOp) : op.token = "||" ↔ op = .lor := by
  cases op <;> decide

/-- Parentheses do not leave a trace in the CPG. -/
theorem translate_unparen (ctx : Ctx) : ∀ e : Expr, translate ctx e.unparen = translate ctx e
  | .paren _ x => by simp only [Expr.unparen, translate]; exact translate_unparen ctx x
  | .basicLit .. | .ident .. | .binary .. | .unary .. | .call .. | .selector .. | .index ..
  | .slice .. | .star .. | .unsupported .. => rfl

theorem unparen_not_paren (span : Cpg.Span) (x : Expr) : ∀ e : Expr, e.unparen ≠ .paren span x
  | .paren _ y => by simp only [Expr.unparen]; exact unparen_not_paren span x y
  | .basicLit .. | .ident .. | .binary .. | .unary .. | .call .. | .selector .. | .index ..
  | .slice .. | .star .. | .unsupported .. => by simp [Expr.unparen]

/-- The translation never produces a bare `Range`; ranges only occur inside subscriptions. -/
theorem translate_ne_range (ctx : Ctx) (e : Expr) (loc : Cpg.Span) (a b c : Option Cpg.Expr) :
    translate ctx e ≠ .range loc a b c := by
  cases e with
  | ident => simp only [translate, translateIdent]; split <;> (try split) <;> (try split) <;> simp
  | basicLit => simp only [translate]; split <;> simp
  | call _ fn _ =>
    cases fn <;> simp only [translate] <;> (try split) <;> (try split) <;> simp
  | selector => simp only [translate]; split <;> simp
  | paren _ x => simp only [translate]; exact translate_ne_range ctx x loc a b c
  | _ => simp [translate]

theorem packageOf?_mem {packages : List String} {x : Expr} {p : String}
    (h : packageOf? packages x = some p) : p ∈ packages := by
  unfold packageOf? at h
  split at h
  · split at h <;> simp_all
  · contradiction

/-- A predeclared identifier that is not one of the special constants has no value. -/
theorem predeclared_other (iota : Option Int) (name : String)
    (h : name ∉ ["true", "false", "nil", "iota"]) : predeclared iota name = none := by
  unfold predeclared
  split <;> simp_all

/-- The value of an identifier is preserved. -/
theorem translateIdent_correct (ctx : Ctx) (genv : Env) (cenv : Cpg.Env)
    (h : EnvAgree ctx genv cenv) (span : Cpg.Span) (name : String) :
    (translateIdent ctx span name).eval semantics cenv
      = (Expr.ident span name).eval ctx.iota ctx.packages genv := by
  unfold translateIdent
  by_cases hs : name ∈ ctx.shadowed
  · simp only [hs, ite_true, Cpg.Expr.eval, Expr.eval, h.vars]
    cases hv : genv.vars name
    · simp [h.shadowedDeclared name hs hv]
    · rfl
  simp only [hs, ite_false]
  split
  · simp [Cpg.Expr.eval, Expr.eval, (h.constsUnshadowed "true" (by simp) hs).1, predeclared]
  · simp [Cpg.Expr.eval, Expr.eval, (h.constsUnshadowed "false" (by simp) hs).1, predeclared]
  · simp [Cpg.Expr.eval, Expr.eval, (h.constsUnshadowed "nil" (by simp) hs).1, predeclared]
  · split <;>
      simp_all [Cpg.Expr.eval, Expr.eval, (h.constsUnshadowed "iota" (by simp) hs).1, predeclared]
  · rename_i h1 h2 h3 h4
    have hp := predeclared_other ctx.iota name (by simp_all)
    simp only [Cpg.Expr.eval, Expr.eval, h.vars, hp]
    split <;> simp_all

/-- Calling a translated identifier calls the same function as in Go. -/
theorem call_ident_correct (ctx : Ctx) (genv : Env) (cenv : Cpg.Env)
    (h : EnvAgree ctx genv cenv) (span s : Cpg.Span) (name : String) (args : List Cpg.Expr)
    (vs : Option (List Value)) (hargs : Cpg.Expr.evalList semantics cenv args = vs) :
    (Cpg.Expr.call span (translateIdent ctx s name) args).eval semantics cenv
      = (do let f ← genv.funcs name; f (← vs)) := by
  unfold translateIdent
  by_cases hs : name ∈ ctx.shadowed
  · simp only [hs, ite_true, Cpg.Expr.eval, h.funcs, hargs]
  simp only [hs, ite_false]
  split
  · simp [Cpg.Expr.eval, (h.constsUnshadowed "true" (by simp) hs).2]
  · simp [Cpg.Expr.eval, (h.constsUnshadowed "false" (by simp) hs).2]
  · simp [Cpg.Expr.eval, (h.constsUnshadowed "nil" (by simp) hs).2]
  · split <;> simp [Cpg.Expr.eval, (h.constsUnshadowed "iota" (by simp) hs).2]
  · simp only [Cpg.Expr.eval, h.funcs, hargs]

/-- A subscription whose subscript is not a `Range` is an indexing. -/
theorem eval_subscription_index (L : Cpg.LanguageSemantics) (env : Cpg.Env) (loc : Cpg.Span)
    (arr idx : Cpg.Expr) (h : ∀ l a b c, idx ≠ .range l a b c) :
    (Cpg.Expr.subscription loc arr idx).eval L env
      = (do env.index (← arr.eval L env) (← idx.eval L env)) := by
  cases idx with
  | range l a b c => exact absurd rfl (h l a b c)
  | _ => simp only [Cpg.Expr.eval]

/-- A call of a named function is preserved, given that its arguments are. -/
theorem call_ident_full_correct (ctx : Ctx) (genv : Env) (cenv : Cpg.Env)
    (h : EnvAgree ctx genv cenv) (span s : Cpg.Span) (name : String) (args : List Cpg.Expr)
    (vs : Option (List Value)) (hargs : Cpg.Expr.evalList semantics cenv args = vs) :
    (if (name == "new" || name == "make") && !ctx.shadowed.contains name then
        Cpg.Expr.problem span "new and make are not in the verified subset"
      else Cpg.Expr.call span (translateIdent ctx s name) args).eval semantics cenv
      = (do let f ← genv.funcs name; f (← vs)) := by
  split
  · rename_i halloc
    simp only [Bool.and_eq_true, Bool.not_eq_true'] at halloc
    obtain ⟨hname, hs⟩ := halloc
    have hs' : name ∉ ctx.shadowed := by simpa using hs
    have hf := h.allocUnshadowed name (by simpa using hname) hs'
    simp [Cpg.Expr.eval, hf]
  · exact call_ident_correct ctx genv cenv h span s name _ _ hargs

/-- A call whose callee is not a selector is preserved, given that its arguments are. -/
theorem call_other_correct (ctx : Ctx) (genv : Env) (cenv : Cpg.Env)
    (h : EnvAgree ctx genv cenv) (span : Cpg.Span) (fn : Expr) (args : List Expr)
    (hfn : ∀ s x sel, fn ≠ .selector s x sel)
    (ihargs : Cpg.Expr.evalList semantics cenv (translateList ctx args)
      = Expr.evalList ctx.iota ctx.packages genv args) :
    (translate ctx (.call span fn args)).eval semantics cenv
      = (Expr.call span fn args).eval ctx.iota ctx.packages genv := by
  cases fn with
  | selector s x sel => exact absurd rfl (hfn s x sel)
  | ident s name =>
    simp only [translate, Expr.eval, calleeName?, Expr.unparen]
    exact call_ident_full_correct ctx genv cenv h span s name _ _ ihargs
  | paren s y =>
    simp only [translate, Expr.eval, calleeName?, Expr.unparen]
    rw [← translate_unparen ctx y]
    generalize hu : y.unparen = u
    cases u with
    | ident s' name =>
      simp only [translate]
      exact call_ident_full_correct ctx genv cenv h span s' name _ _ ihargs
    | paren s' x => exact absurd hu (unparen_not_paren s' x y)
    | _ => simp [Cpg.Expr.eval]
  | _ => simp [translate, calleeName?, Expr.unparen, Expr.eval, Cpg.Expr.eval]

mutual

/--
**Semantic preservation.** Evaluating the translated CPG expression yields the same result as
evaluating the Go expression.
-/
theorem translate_correct (ctx : Ctx) (genv : Env) (cenv : Cpg.Env)
    (h : EnvAgree ctx genv cenv) :
    ∀ e : Expr, (translate ctx e).eval semantics cenv = e.eval ctx.iota ctx.packages genv
  | .basicLit span kind value => by
    simp only [translate, Expr.eval]
    split <;> simp_all [Cpg.Expr.eval]
  | .ident span name => by
    simp only [translate]
    exact translateIdent_correct ctx genv cenv h span name
  | .binary span x op y => by
    have ihx := translate_correct ctx genv cenv h x
    have ihy := translate_correct ctx genv cenv h y
    by_cases hl : op = .land
    · subst hl
      simp only [translate, BinaryOp.token, Cpg.Expr.eval, Expr.eval, ihx, ihy]
      rfl
    by_cases hr : op = .lor
    · subst hr
      simp only [translate, BinaryOp.token, Cpg.Expr.eval, Expr.eval, ihx, ihy]
      rfl
    have hl' : op.token ≠ "&&" := fun e => hl ((token_eq_land op).1 e)
    have hr' : op.token ≠ "||" := fun e => hr ((token_eq_lor op).1 e)
    have hgo : (Expr.binary span x op y).eval ctx.iota ctx.packages genv
        = (do evalBinary op (← x.eval ctx.iota ctx.packages genv) (← y.eval ctx.iota ctx.packages genv)) := by
      cases op <;> first | contradiction | rfl
    rw [hgo]
    simp only [translate, Cpg.Expr.eval, hl', hr', ite_false, ihx, ihy,
      evalBinaryOp_token op hl hr]
  | .unary span op x => by
    have ihx := translate_correct ctx genv cenv h x
    simp only [translate, Cpg.Expr.eval, Expr.eval, ihx, evalUnaryOp_token]
  | .paren span x => by
    simp only [translate, Expr.eval]
    exact translate_correct ctx genv cenv h x
  | .selector span x sel => by
    have ihx := translate_correct ctx genv cenv h x
    simp only [translate, Expr.eval]
    cases hp : packageOf? ctx.packages x with
    | some p => simp only [Cpg.Expr.eval, h.qualifiedVars p (packageOf?_mem hp) sel]
    | none => simp only [Cpg.Expr.eval, ihx, h.fields]; rfl
  | .call span fn args => by
    have ihargs := translateList_correct ctx genv cenv h args
    match fn with
    | .selector s x sel =>
      have ihx := translate_correct ctx genv cenv h x
      simp only [translate, Expr.eval]
      cases hp : packageOf? ctx.packages x with
      | some p =>
        simp only [Cpg.Expr.eval, ihargs, h.qualifiedFuncs p (packageOf?_mem hp) sel]
      | none => simp only [Cpg.Expr.eval, ihx, ihargs, h.methods]
    | .basicLit .. | .ident .. | .binary .. | .unary .. | .paren .. | .call .. | .index ..
    | .slice .. | .star .. | .unsupported .. =>
      exact call_other_correct ctx genv cenv h span _ args (by simp) ihargs
  | .index span x i => by
    have ihx := translate_correct ctx genv cenv h x
    have ihi := translate_correct ctx genv cenv h i
    simp only [translate, Expr.eval]
    rw [eval_subscription_index _ _ _ _ _ (translate_ne_range ctx i)]
    simp only [ihx, ihi, h.index]
  | .slice span x low high max => by
    have ihx := translate_correct ctx genv cenv h x
    have ihl := translateOpt_correct ctx genv cenv h low
    have ihh := translateOpt_correct ctx genv cenv h high
    have ihm := translateOpt_correct ctx genv cenv h max
    simp only [translate, Cpg.Expr.eval, Expr.eval, ihx, ihl, ihh, ihm, h.slice]
  | .star span x => by
    have ihx := translate_correct ctx genv cenv h x
    simp only [translate, Cpg.Expr.eval, Expr.eval, ihx, h.deref]
  | .unsupported span goType => by simp [translate, Cpg.Expr.eval, Expr.eval]

/-- Semantic preservation for optional expressions. -/
theorem translateOpt_correct (ctx : Ctx) (genv : Env) (cenv : Cpg.Env)
    (h : EnvAgree ctx genv cenv) :
    ∀ e : Option Expr,
      Cpg.Expr.evalOpt semantics cenv (translateOpt ctx e)
        = Expr.evalOpt ctx.iota ctx.packages genv e
  | none => by simp [translateOpt, Cpg.Expr.evalOpt, Expr.evalOpt]
  | some e => by
    simp only [translateOpt, Cpg.Expr.evalOpt, Expr.evalOpt, translate_correct ctx genv cenv h e]

/-- Semantic preservation for lists of expressions. -/
theorem translateList_correct (ctx : Ctx) (genv : Env) (cenv : Cpg.Env)
    (h : EnvAgree ctx genv cenv) :
    ∀ es : List Expr,
      Cpg.Expr.evalList semantics cenv (translateList ctx es)
        = Expr.evalList ctx.iota ctx.packages genv es
  | [] => by simp [translateList, Cpg.Expr.evalList, Expr.evalList]
  | e :: es => by
    simp only [translateList, Cpg.Expr.evalList, Expr.evalList,
      translate_correct ctx genv cenv h e, translateList_correct ctx genv cenv h es]

end

/-- **Location preservation.** Every translated node is located at its (unparenthesized) source. -/
theorem translate_loc (ctx : Ctx) : ∀ e : Expr, (translate ctx e).loc = e.unparen.span
  | .basicLit .. => by simp only [translate]; split <;> rfl
  | .ident span name => by
    simp only [translate, translateIdent]
    split
    · rfl
    split <;> try rfl
    split <;> rfl
  | .binary .. | .unary .. | .index .. | .slice .. | .star .. | .unsupported .. => by
    simp only [translate, Cpg.Expr.loc, Expr.unparen, Expr.span]
  | .paren _ x => by simpa [translate, Expr.unparen] using translate_loc ctx x
  | .selector .. => by
    simp only [translate]
    split <;> rfl
  | .call _ fn _ => by
    cases fn <;> simp only [translate] <;> (try split) <;> (try split) <;> rfl

end Go
