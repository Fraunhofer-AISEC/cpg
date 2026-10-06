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
  /-- The predeclared constants are not shadowed. -/
  constsUnshadowed : ∀ name ∈ ["true", "false", "nil", "iota"],
    genv.vars name = none ∧ genv.funcs name = none
  /-- The built-in allocation functions are not shadowed. -/
  allocUnshadowed : genv.funcs "new" = none ∧ genv.funcs "make" = none

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
  | .basicLit .. | .ident .. | .binary .. | .unary .. | .call .. | .unsupported .. => rfl

theorem unparen_not_paren (span : Cpg.Span) (x : Expr) : ∀ e : Expr, e.unparen ≠ .paren span x
  | .paren _ y => by simp only [Expr.unparen]; exact unparen_not_paren span x y
  | .basicLit .. | .ident .. | .binary .. | .unary .. | .call .. | .unsupported .. => by
    simp [Expr.unparen]

/-- A predeclared identifier that is not one of the special constants has no value. -/
theorem predeclared_other (iota : Option Int) (name : String)
    (h : name ∉ ["true", "false", "nil", "iota"]) : predeclared iota name = none := by
  unfold predeclared
  split <;> simp_all

/-- The value of an identifier is preserved. -/
theorem translateIdent_correct (ctx : Ctx) (genv : Env) (cenv : Cpg.Env)
    (h : EnvAgree ctx genv cenv) (span : Cpg.Span) (name : String) :
    (translateIdent ctx span name).eval semantics cenv
      = (Expr.ident span name).eval ctx.iota genv := by
  unfold translateIdent
  split
  · simp [Cpg.Expr.eval, Expr.eval, (h.constsUnshadowed "true" (by simp)).1, predeclared]
  · simp [Cpg.Expr.eval, Expr.eval, (h.constsUnshadowed "false" (by simp)).1, predeclared]
  · simp [Cpg.Expr.eval, Expr.eval, (h.constsUnshadowed "nil" (by simp)).1, predeclared]
  · split <;>
      simp_all [Cpg.Expr.eval, Expr.eval, (h.constsUnshadowed "iota" (by simp)).1, predeclared]
  · rename_i h1 h2 h3 h4
    have hp := predeclared_other ctx.iota name (by simp_all)
    simp only [Cpg.Expr.eval, Expr.eval, h.vars, hp]
    split <;> simp_all

/-- Calling a translated callee behaves like calling the Go callee. -/
theorem call_callee_correct (ctx : Ctx) (genv : Env) (cenv : Cpg.Env)
    (h : EnvAgree ctx genv cenv) (span : Cpg.Span) (fn : Expr) (args : List Cpg.Expr)
    (vs : Option (List Value)) (hargs : Cpg.Expr.evalList semantics cenv args = vs)
    (halloc : isAllocation fn = false) :
    (Cpg.Expr.call span (translate ctx fn) args).eval semantics cenv
      = (match fn.unparen with
        | .ident _ name => do
          let f ← genv.funcs name
          f (← vs)
        | _ => none) := by
  rw [← translate_unparen]
  unfold isAllocation at halloc
  generalize hu : fn.unparen = u at halloc ⊢
  cases u with
  | ident s name =>
    simp only [translate]
    unfold translateIdent
    split
    · simp [Cpg.Expr.eval, (h.constsUnshadowed "true" (by simp)).2]
    · simp [Cpg.Expr.eval, (h.constsUnshadowed "false" (by simp)).2]
    · simp [Cpg.Expr.eval, (h.constsUnshadowed "nil" (by simp)).2]
    · split <;> simp [Cpg.Expr.eval, (h.constsUnshadowed "iota" (by simp)).2]
    · simp only [Cpg.Expr.eval, h.funcs, hargs]
  | paren s x => exact absurd hu (unparen_not_paren s x fn)
  | basicLit s k v =>
    simp only [translate]
    split <;> simp [Cpg.Expr.eval]
  | binary => simp [translate, Cpg.Expr.eval]
  | unary => simp [translate, Cpg.Expr.eval]
  | call s fn' args' =>
    simp only [translate]
    split <;> simp [Cpg.Expr.eval]
  | unsupported => simp [translate, Cpg.Expr.eval]

mutual

/--
**Semantic preservation.** Evaluating the translated CPG expression yields the same result as
evaluating the Go expression.
-/
theorem translate_correct (ctx : Ctx) (genv : Env) (cenv : Cpg.Env)
    (h : EnvAgree ctx genv cenv) :
    ∀ e : Expr, (translate ctx e).eval semantics cenv = e.eval ctx.iota genv
  | .basicLit span kind value => by
    simp only [translate, Expr.eval]
    split <;> simp_all [Cpg.Expr.eval]
  | .ident span name => translateIdent_correct ctx genv cenv h span name
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
    have hgo : (Expr.binary span x op y).eval ctx.iota genv
        = (do evalBinary op (← x.eval ctx.iota genv) (← y.eval ctx.iota genv)) := by
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
  | .call span fn args => by
    have ihargs := translateList_correct ctx genv cenv h args
    simp only [translate, Expr.eval]
    split
    · rename_i halloc
      unfold isAllocation at halloc
      simp only [Cpg.Expr.eval]
      split at halloc
      · rename_i s name hu
        rw [hu]
        have : name = "new" ∨ name = "make" := by simpa using halloc
        rcases this with rfl | rfl <;> simp [h.allocUnshadowed]
      · contradiction
    · rename_i halloc
      exact call_callee_correct ctx genv cenv h span fn _ _ ihargs (by simpa using halloc)
  | .unsupported span goType => by simp [translate, Cpg.Expr.eval, Expr.eval]

/-- Semantic preservation for lists of expressions. -/
theorem translateList_correct (ctx : Ctx) (genv : Env) (cenv : Cpg.Env)
    (h : EnvAgree ctx genv cenv) :
    ∀ es : List Expr,
      Cpg.Expr.evalList semantics cenv (translateList ctx es) = Expr.evalList ctx.iota genv es
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
    split <;> try rfl
    split <;> rfl
  | .binary .. | .unary .. | .unsupported .. => rfl
  | .paren _ x => by simpa [translate, Expr.unparen] using translate_loc ctx x
  | .call .. => by
    simp only [translate]
    split <;> rfl

end Go
