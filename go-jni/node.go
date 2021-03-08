package cpg

import (
	"bytes"
	"go/ast"
	"go/printer"
	"go/token"
	"log"

	"tekao.net/jnigi"
)

type Node jnigi.ObjectRef

func (n *Node) SetName(env *jnigi.Env, s string) error {
	return (*jnigi.ObjectRef)(n).SetField(env, "name", NewString(env, s))
}

func (n *Node) SetCode(env *jnigi.Env, s string) error {
	return (*jnigi.ObjectRef)(n).SetField(env, "code", NewString(env, s))
}

func (n *Node) GetName(env *jnigi.Env) string {
	o, _ := (*jnigi.ObjectRef)(n).CallMethod(env, "getName", jnigi.ObjectType("java/lang/String"))

	if o == nil {
		return ""
	}

	b, err := o.(*jnigi.ObjectRef).CallMethod(env, "getBytes", jnigi.Byte|jnigi.Array)
	if err != nil {
		log.Fatal(err)
	}

	return string(b.([]byte))
}

func updateCode(fset *token.FileSet, env *jnigi.Env, node *Node, astNode ast.Node) {
	var codeBuf bytes.Buffer
	_ = printer.Fprint(&codeBuf, fset, astNode)

	node.SetCode(env, codeBuf.String())
}
