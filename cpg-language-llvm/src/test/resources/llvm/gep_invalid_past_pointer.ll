; This file is intentionally invalid LLVM IR, used to confirm that LLVM itself rejects indexing
; past a pointer-typed field within a single getelementptr instruction. %struct.Node's field_1 is
; a self-pointer, resolved to UnknownType by typeOf's recursive-type cycle guard (independent of
; opaque pointers). The GEP loop in ExpressionHandler.handleGetElementPtr therefore treats
; UnknownType the same as PointerType (see the comment there) -- but that branch can only ever be
; reached for the *last* index of a chain, because LLVM's own parser already rejects a GEP that
; continues indexing after a pointer field, as this file demonstrates.
;
; LLVM's parser error for this file is "invalid getelementptr indices".

%struct.Node = type { i32, %struct.Node* }

define i32 @getNextNext(%struct.Node* %n) {
  %p = getelementptr %struct.Node, %struct.Node* %n, i64 0, i32 1, i32 0
  %v = load i32, i32* %p
  ret i32 %v
}
