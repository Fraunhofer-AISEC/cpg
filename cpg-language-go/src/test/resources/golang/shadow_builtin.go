package p

// int is not a keyword, but a predeclared identifier. This alias shadows it in package p.
type int = string

var shadowed int

var pointer *int

var converted = int("text")

// error is not shadowed, so it stays the predeclared type
var untouched error
