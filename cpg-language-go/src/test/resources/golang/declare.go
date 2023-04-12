package main

import "fmt"

func main() {
	// Declaring multiple variables in a block with initializer. This is one
	// GenDecl with two ValueSpec specs and one "Name" and (initializer) "Value"
	// each.
	//
	// We translate this into one DeclarationStatement with two
	// VariableDeclaration nodes
	var (
		a int = 1
		b int = 2
	)

	// Declaring multiple variables in a single line. This is one GenDecl with
	// one ValueSpec spec which contains two "Names" and two "Values" which
	// correspond to the respective initializer values. Note, that the number of
	// values MUST match the number of names
	//
	// We translate this into one DeclarationStatement with two
	// VariableDeclaration nodes
	var c, d = 3, 4

	// Short assignment using an assignment, where all variables were not
	// defined before. This is an AssignStmt which has DEFINE as its token.
	//
	// We need to split this up into several nodes. First, we translate this
	// into one (implicit) DeclarationStatement with two VariableDeclaration
	// nodes. Afterwards we are parsing it as a regular assignment.
	e, f := 5, 6

	// Short assignment using an assignment, where one variable (f) was defined
	// before in the local scope. This is an AssignStmt which has DEFINE as its
	// token. From the AST we cannot differentiate this from the previous
	// example and we need to do a (local) variable lookup here.
	//
	// Finally, We need to split this up into several nodes. First, we translate
	// this into one (implicit) DeclarationStatement with one
	// VariableDeclaration node. Afterwards we are parsing it as a regular
	// assignment.
	f, g := 7, 8

	fmt.Printf("%d %d %d %d %d %d %d\n", a, b, c, d, e, f, g)
}
