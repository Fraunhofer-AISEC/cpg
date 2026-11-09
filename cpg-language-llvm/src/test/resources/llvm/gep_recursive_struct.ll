; A self-referential struct, accessed with typed pointers (not opaque ones). The self-pointer
; field's type cannot be resolved while the struct itself is still being constructed (the type
; resolver's cycle guard returns UnknownType for it, to avoid infinite recursion), so this is a
; second, pointer-syntax-independent source of UnknownType in the type system, besides opaque
; pointers. Since the field is itself pointer-typed, GEP cannot index past it in a single
; instruction either way, so this does not hit the "could not find structure type" failure mode.
;
; struct Node {
;   int value;
;   struct Node *next;
; };
;
; struct Node *getNext(struct Node *n) {
;   return n->next;
; }

%struct.Node = type { i32, %struct.Node* }

define %struct.Node* @getNext(%struct.Node* %n) {
  %p = getelementptr %struct.Node, %struct.Node* %n, i64 0, i32 1
  %next = load %struct.Node*, %struct.Node** %p
  ret %struct.Node* %next
}
