package main

// A package-level map whose values are function literals, mirroring miekg/dns'
// generated ztypes.go `TypeToRR`. The literals are anonymous functions declared
// at package scope rather than inside a function body.
var TypeToRR = map[uint16]func() RR{
	1: func() RR { return new(A) },
	2: func() RR { return new(AAAA) },
	3: func() RR { return new(OPT) },
}

type RR interface {
	Header() *RRHeader
	copy() RR
}

type RRHeader struct {
	Name string
}

type A struct {
	Hdr RRHeader
}

type AAAA struct {
	Hdr RRHeader
}

type OPT struct {
	Hdr RRHeader
}

// The methods below are declared *after* the map. In ztypes.go the equivalent
// methods are the only place where most RR types get `Header` and `copy`, and
// losing them removes those types from the `RR` interface entirely. They are
// also written as one-liners, as the generator emits them.
func (rr *A) Header() *RRHeader    { return &rr.Hdr }
func (rr *AAAA) Header() *RRHeader { return &rr.Hdr }
func (rr *OPT) Header() *RRHeader  { return &rr.Hdr }

func (rr *A) copy() RR    { return &A{Hdr: rr.Hdr} }
func (rr *AAAA) copy() RR { return &AAAA{Hdr: rr.Hdr} }
func (rr *OPT) copy() RR  { return &OPT{Hdr: rr.Hdr} }

func useRR(r RR) *RRHeader {
	return r.Header()
}

func main() {
	useRR(&OPT{})
}
