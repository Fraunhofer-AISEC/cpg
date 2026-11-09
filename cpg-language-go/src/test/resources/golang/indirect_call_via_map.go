package main

type RR interface {
	Header() int
}

type A struct{}

func (a *A) Header() int { return headerOfA() }

func headerOfA() int { return 1 }

// A package-level map of constructors, as in miekg/dns' generated ztypes.go.
// The function literals are only ever invoked indirectly, through a value read
// back out of the map.
var TypeToRR = map[uint16]func() RR{
	1: func() RR { return new(A) },
}

// mirrors dns.UnpackRRWithHeader
func unpack(t uint16) RR {
	if newFn, ok := TypeToRR[t]; ok {
		return newFn()
	}
	return nil
}

// The simplest possible variant: a local variable holding a literal.
func direct() RR {
	fn := func() RR { return new(A) }
	return fn()
}

// A literal passed as an argument and invoked by the callee.
func apply(f func() RR) RR { return f() }

func viaParam() RR {
	return apply(func() RR { return new(A) })
}

func main() {
	unpack(1)
	direct()
	viaParam()
}
