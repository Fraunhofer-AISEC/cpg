package cpg

import "tekao.net/jnigi"

type ScopeManager jnigi.ObjectRef
type Scope jnigi.ObjectRef

func (s *ScopeManager) EnterScope(env *jnigi.Env, n *Node) {
	(*jnigi.ObjectRef)(s).CallMethod(env, "enterScope", jnigi.Void, (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))
}

func (s *ScopeManager) LeaveScope(env *jnigi.Env, n *Node) {
	(*jnigi.ObjectRef)(s).CallMethod(env, "leaveScope", jnigi.ObjectType("de/fraunhofer/aisec/cpg/passes/scopes/Scope"), (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))
}

func (s *ScopeManager) ResetToGlobal(env *jnigi.Env, n *Node) {
	(*jnigi.ObjectRef)(s).CallMethod(env, "resetToGlobal", jnigi.Void, (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/declarations/TranslationUnitDeclaration"))
}

func (s *ScopeManager) GetCurrentScope(env *jnigi.Env) *Scope {
	o, _ := (*jnigi.ObjectRef)(s).CallMethod(env, "getCurrentScope", jnigi.ObjectType("de/fraunhofer/aisec/cpg/passes/scopes/Scope"))

	return (*Scope)(o.(*jnigi.ObjectRef))
}

func (s *ScopeManager) GetCurrentFunction(env *jnigi.Env) *FunctionDeclaration {
	o, _ := (*jnigi.ObjectRef)(s).CallMethod(env, "getCurrentFunction", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/declarations/FunctionDeclaration"))

	return (*FunctionDeclaration)(o.(*jnigi.ObjectRef))
}

func (s *ScopeManager) GetRecordForName(env *jnigi.Env, scope *Scope, recordName string) (record *RecordDeclaration, err error) {
	var o interface{}

	o, err = (*jnigi.ObjectRef)(s).CallMethod(env,
		"getRecordForName",
		jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/declarations/RecordDeclaration"),
		(*jnigi.ObjectRef)(scope).Cast("de/fraunhofer/aisec/cpg/passes/scopes/Scope"),
		NewString(env, recordName))

	record = (*RecordDeclaration)(o.(*jnigi.ObjectRef))

	return
}

func (s *ScopeManager) AddDeclaration(env *jnigi.Env, d *Declaration) (err error) {
	_, err = (*jnigi.ObjectRef)(s).CallMethod(env, "addDeclaration", jnigi.Void, (*jnigi.ObjectRef)(d).Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))

	return
}
