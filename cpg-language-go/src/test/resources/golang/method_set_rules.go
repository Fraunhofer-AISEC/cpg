package main

// Rule 4: a struct may embed an *interface*; its methods are promoted.
type Reader interface{ Read(p []byte) int }
type Closer interface{ Close() error }

// ReadCloser embeds two interfaces -> its method set is the union (rule 3).
type ReadCloser interface {
	Reader
	Closer
}

// wrapper embeds an interface value, so it satisfies Reader through promotion.
type wrapper struct {
	Reader
	name string
}

// base/derived: embedding a struct, transitively (rule 4).
type base struct{}

func (b base) Read(p []byte) int { return readImpl() }
func readImpl() int              { return 0 }

type middle struct{ base }
type outer struct{ middle }

// pointerEmbed embeds a *pointer* to a struct.
type pointerEmbed struct{ *base }

func consume(r Reader) int { return r.Read(nil) }

func main() {
	consume(wrapper{})
	consume(outer{})
	consume(pointerEmbed{})
}
