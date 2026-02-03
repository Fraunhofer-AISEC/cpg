package p

import (
	"mymodule/submodule"
)

type MyStruct struct {
	Field int
}

const a = 1

const (
	b int = iota
)

const s = "test"
const f = 1.0
const f32 float32 = 1.00

var n *int = nil

var fn = func(_ int) int {
	return 1
}

type structArray []MyStruct

// o is a composite literal. It also demonstrates that we can omit the
// type specifier of an "inner" composite literal, if the outer one is an array type
var o = []MyStruct{{Field: 10}}

// o is similar to o2, but with a pointer type
var o2 = []*MyStruct{{Field: 10}}

// o3 is similar to o3, but with a new type that uses []MyStruct as underlying type
var o3 = structArray{{Field: 10}}

var rr = []int{
	submodule.Zero: 1,
}

var rr2 = [...]int{
	submodule.Zero: 1,
}

var mapr = map[int][]byte{
	submodule.Zero: {1, 2, 3},
}

var structr = []MyStruct{
	0: {Field: 10},
}

type pairNameValue struct {
	name, value string
}

var structKey = map[pairNameValue]uint64{
	{name: "this", value: "that"}: 1,
	{name: "that", value: "this"}: 2,
}

var structValue = map[uint64]pairNameValue{
	1: {name: "this", value: "that"},
}
