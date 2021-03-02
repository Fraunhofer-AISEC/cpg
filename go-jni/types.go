package cpg

import (
	"C"

	"tekao.net/jnigi"
)

type Type jnigi.ObjectRef

func TypeParser_createFrom(env *jnigi.Env, s string, resolveAlias bool) *Type {
	//_, err = (*jnigi.ObjectRef)(t).CallMethod(env, "addDeclaration", jnigi.Void, d.Cast("de/fraunhofer/aisec/cpg/graph/declarations/Declaration"))
	t, err := env.CallStaticMethod("de/fraunhofer/aisec/cpg/graph/types/TypeParser", "createFrom", jnigi.ObjectType("de/fraunhofer/aisec/cpg/graph/types/Type"), NewString(env, s), resolveAlias)
	if err != nil {
		panic(err)
	}

	return (*Type)(t.(*jnigi.ObjectRef))
}
