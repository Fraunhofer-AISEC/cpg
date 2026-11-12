package asn1

// ObjectIdentifier in the "standard" package.
type ObjectIdentifier []int

func (oi ObjectIdentifier) String() string { return stdString() }

func stdString() string { return "std" }
