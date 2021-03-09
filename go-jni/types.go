package cpg

import (
	"C"

	"tekao.net/jnigi"
)
import "log"

type Type jnigi.ObjectRef
type HasType jnigi.ObjectRef

func TypeParser_createFrom(env *jnigi.Env, s string, resolveAlias bool) *Type {
	//_, err = (*jnigi.ObjectRef)(t).CallMethod(env, "addDeclaration", jnigi.Void, d.Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))
	t, err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/types/TypeParser", "createFrom", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/Type"), NewString(env, s), resolveAlias)
	if err != nil {
		panic(err)
	}

	return (*Type)(t.(*jnigi.ObjectRef))
}

func (h *HasType) SetType(env *jnigi.Env, t *Type) {
	if t != nil {
		(*jnigi.ObjectRef)(h).CallMethod(env, "setType", jnigi.Void, (*jnigi.ObjectRef)(t))
	}
}

func (h *HasType) GetType(env *jnigi.Env) *Type {
	i, err := (*jnigi.ObjectRef)(h).CallMethod(env, "getType", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/Type"))
	if err != nil {
		log.Fatal(err)
	}

	return (*Type)(i.(*jnigi.ObjectRef))
}
