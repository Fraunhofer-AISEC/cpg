package cpg

import (
	"log"

	"tekao.net/jnigi"
)

type Node jnigi.ObjectRef

func (n *Node) SetName(env *jnigi.Env, s string) error {
	return (*jnigi.ObjectRef)(n).SetField(env, "name", NewString(env, s))
}

func (n *Node) GetName(env *jnigi.Env) string {
	o, _ := (*jnigi.ObjectRef)(n).CallMethod(env, "getName", jnigi.ObjectType("java/lang/String"))

	b, err := o.(*jnigi.ObjectRef).CallMethod(env, "getBytes", jnigi.Byte|jnigi.Array)
	if err != nil {
		log.Fatal(err)
	}

	return string(b.([]byte))
}
