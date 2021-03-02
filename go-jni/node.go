package cpg

import "tekao.net/jnigi"

type Node jnigi.ObjectRef

func (n *Node) SetName(env *jnigi.Env, s string) error {
	return (*jnigi.ObjectRef)(n).SetField(env, "name", NewString(env, s))
}
