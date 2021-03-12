package cpg

import (
	"C"

	"tekao.net/jnigi"
)
import "log"

var env *jnigi.Env

type Type jnigi.ObjectRef
type HasType jnigi.ObjectRef

func InitEnv(e *jnigi.Env) {
	env = e
}

func TypeParser_createFrom(s string, resolveAlias bool) *Type {
	t, err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/types/TypeParser", "createFrom", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/Type"), NewString(s), resolveAlias)
	if err != nil {
		panic(err)
	}

	return (*Type)(t.(*jnigi.ObjectRef))
}

func UnknownType_getUnknown() *Type {
	t, err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/types/UnknownType", "getUnknownType", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/UnknownType"))
	if err != nil {
		panic(err)
	}

	return (*Type)(t.(*jnigi.ObjectRef))
}

func (h *Type) GetRoot() *Type {
	o, err := (*jnigi.ObjectRef)(h).CallMethod(env, "getRoot", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/Type"))
	if err != nil {
		panic(err)
	}

	return (*Type)(o.(*jnigi.ObjectRef))
}

func (h *HasType) SetType(t *Type) {
	if t != nil {
		(*jnigi.ObjectRef)(h).CallMethod(env, "setType", jnigi.Void, (*jnigi.ObjectRef)(t))
	}
}

func (h *HasType) GetType() *Type {
	i, err := (*jnigi.ObjectRef)(h).CallMethod(env, "getType", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/Type"))
	if err != nil {
		log.Fatal(err)
	}

	return (*Type)(i.(*jnigi.ObjectRef))
}
