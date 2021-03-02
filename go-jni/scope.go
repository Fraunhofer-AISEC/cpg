package cpg

import "tekao.net/jnigi"

type ScopeManager jnigi.ObjectRef

func (s *ScopeManager) EnterScope(env *jnigi.Env, n *Node) {
	(*jnigi.ObjectRef)(s).CallMethod(env, "enterScope", jnigi.Void, (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))
}

func (s *ScopeManager) LeaveScope(env *jnigi.Env, n *Node) {
	(*jnigi.ObjectRef)(s).CallMethod(env, "leaveScope", jnigi.ObjectType("de/fraunhofer/aisec/cpg/passes/scopes/Scope"), (*jnigi.ObjectRef)(n).Cast("de/fraunhofer/aisec/cpg/graph/Node"))
}

func (s *ScopeManager) AddDeclaration(env *jnigi.Env, d *Declaration) {
	(*jnigi.ObjectRef)(s).CallMethod(env, "addDeclaration", jnigi.Void, (*jnigi.ObjectRef)(d).Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))
}
