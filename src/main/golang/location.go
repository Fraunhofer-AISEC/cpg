package cpg

import (
	"go/ast"
	"go/token"
	"log"

	"tekao.net/jnigi"
)

type PhysicalLocation jnigi.ObjectRef
type Region jnigi.ObjectRef

func NewRegion(fset *token.FileSet, astNode ast.Node, startLine int, startColumn int, endLine int, endColumn int) *Region {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/sarif/Region", startLine, startColumn, endLine, endColumn)
	if err != nil {
		log.Fatal(err)

	}

	return (*Region)(c)
}

func NewPhysicalLocation(fset *token.FileSet, astNode ast.Node, uri *jnigi.ObjectRef, region *Region) *PhysicalLocation {
	c, err := env.NewObject("de/fraunhofer/aisec/cpg/sarif/PhysicalLocation", (*jnigi.ObjectRef)(uri), (*jnigi.ObjectRef)(region))
	if err != nil {
		log.Fatal(err)

	}

	return (*PhysicalLocation)(c)
}
