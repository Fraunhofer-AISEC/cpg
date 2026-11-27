package main

// This file sorts before the file declaring the types, so the methods below are
// parsed before their receiver types exist. This is the shape of miekg/dns'
// generated ztypes.go, which declares Header/copy/len for nearly every resource
// record type while the types live in types.go.

func (rr *A) Header() *RRHeader { return &rr.Hdr }

func (rr *A) copy() RR { return &A{Hdr: rr.Hdr} }

func (rr *OPT) Header() *RRHeader { return &rr.Hdr }

func (rr *OPT) copy() RR { return &OPT{Hdr: rr.Hdr} }

func useRR(r RR) *RRHeader {
	return r.Header()
}

func main() {
	useRR(&OPT{})
}
