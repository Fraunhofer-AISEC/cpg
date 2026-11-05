package main

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

type OPT struct {
	Hdr RRHeader
}
