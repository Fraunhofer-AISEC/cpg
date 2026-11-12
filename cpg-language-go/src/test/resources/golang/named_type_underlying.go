package main

// Stringer is satisfied structurally by PADataSequence below.
type Stringer interface {
	String() string
}

// PADataSequence is a named type whose underlying type is a slice. Once it also
// satisfies an interface, it has more than one super type.
type PADataSequence []int

func (p PADataSequence) String() string {
	return "seq"
}

func use(pas PADataSequence) int {
	total := 0
	for _, pa := range pas {
		total += pa
	}
	return total
}
