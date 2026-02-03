package p

// Funcy has function type as its underlying type
type Funcy func() error

// newType is a new distinct type, it has string as the "underlying" type.
type newType string

func (n *newType) SomeFunc() {}

var _ = newType("test")

// alias is really a type alias
type alias = string

// s is a variable with a struct type.
var s struct{ Field int }

// i is a variable with an interface type.
var i interface{ MyMethod(int) error }

// m is a variable with an map type.
var m map[int]string

// a is a variable with an array type.
var a []int

// f is of Funcy type
var f Funcy

var g alias

var h newType

func init() {
	_ = s
	_ = i
	_ = m
	_ = a
	_ = f
	_ = g
	_ = h
}
