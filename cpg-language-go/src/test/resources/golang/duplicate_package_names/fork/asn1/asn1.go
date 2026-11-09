package asn1

// ObjectIdentifier in a *fork* -- same package name, different import path.
type ObjectIdentifier []int

func (oi ObjectIdentifier) String() string { return forkString() }

func forkString() string { return "fork" }
