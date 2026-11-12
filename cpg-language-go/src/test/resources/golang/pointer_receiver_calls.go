package main

type Digest struct{ v1, v2 uint64 }

// pointer receiver
func (d *Digest) ResetWithSeed(seed uint64) { d.v1 = seed + prime1 }

const prime1 uint64 = 11400714785074694791

// Case A: value variable calling a pointer-receiver method (Go auto-takes the address
// because `d` is addressable).
func NewWithSeed(seed uint64) *Digest {
	var d Digest
	d.ResetWithSeed(seed)
	return &d
}

// Case B: untyped constant literal passed to a sized integer parameter.
func (d *Digest) Reset() { d.ResetWithSeed(0) }

// Case C: both at once.
func NewZero() *Digest {
	var d Digest
	d.ResetWithSeed(0)
	return &d
}

func main() { NewWithSeed(1); NewZero(); (&Digest{}).Reset() }
