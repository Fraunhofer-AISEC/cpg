package main

// tagAndLength mirrors encoding/asn1: three int fields declared in one entry,
// plus one declared on its own. Keeping only the first name of the group means
// `tag` and `length` do not exist at all.
type tagAndLength struct {
	class, tag, length int
	isCompound         bool
}

type Inner struct {
	a, b string
}

// Embedding must keep working alongside this.
type Outer struct {
	Inner
	x, y int
}

func invalidLength(offset, length, sliceLength int) bool {
	return offset+length < offset || offset+length > sliceLength
}

func use(t tagAndLength, offset int, bytes []byte) bool {
	// t.length must have type int, otherwise this call cannot be matched
	// against invalidLength and resolves to an inferred stub.
	return invalidLength(offset, t.length, len(bytes))
}

func main() {
	use(tagAndLength{class: 1, tag: 2, length: 3}, 0, []byte{})
}
