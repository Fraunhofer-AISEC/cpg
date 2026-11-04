package main

type T struct{}

func (t T) make(n int) int { return n }

const (
	octal    = 0o17
	legacy   = 017
	hexUpper = 0X1F
	binUpper = 0B101
	big      = 0xFFFF_FFFF_FFFF_FFFF
)

var (
	r1 = 'a'
	r2 = '\n'
	r3 = 'é'
	r4 = '\x41'
	s1 = "a\tb"
	s2 = "\xe2\x82\xac"
	s3 = `a\tb`
	notIota = iota
)

func main() {
	var ch = make(chan int, 10)
	var t T
	var m = t.make(1)
	var true = 5
	var x = true
	_ = ch
	_ = m
	_ = x
}
