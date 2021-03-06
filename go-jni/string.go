package cpg

import (
	"log"

	"tekao.net/jnigi"
)

func NewString(env *jnigi.Env, s string) *jnigi.ObjectRef {
	o, err := env.NewObject("java/lang/String", []byte(s))
	if err != nil {
		log.Fatal(err)
	}

	return o
}

func NewInteger(env *jnigi.Env, i int) *jnigi.ObjectRef {
	// TODO: Use Integer.valueOf
	o, err := env.NewObject("java/lang/Integer", i)
	if err != nil {
		log.Fatal(err)
	}

	return o
}
