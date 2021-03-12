package cpg

import (
	"log"

	"tekao.net/jnigi"
)

func NewString(s string) *jnigi.ObjectRef {
	o, err := env.NewObject("java/lang/String", []byte(s))
	if err != nil {
		log.Fatal(err)
	}

	return o
}

func NewInteger(i int) *jnigi.ObjectRef {
	// TODO: Use Integer.valueOf
	o, err := env.NewObject("java/lang/Integer", i)
	if err != nil {
		log.Fatal(err)
	}

	return o
}

func NewDouble(d float64) *jnigi.ObjectRef {
	// TODO: Use Integer.valueOf
	o, err := env.NewObject("java/lang/Double", d)
	if err != nil {
		log.Fatal(err)
	}

	return o
}
