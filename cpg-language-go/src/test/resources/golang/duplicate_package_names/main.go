package main

import (
	stdasn1 "example.com/dup/encoding/asn1"
	forkasn1 "example.com/dup/fork/asn1"
)

type Stringer interface{ String() string }

func use(s Stringer) string { return s.String() }

func main() {
	use(stdasn1.ObjectIdentifier{1})
	use(forkasn1.ObjectIdentifier{2})
}
