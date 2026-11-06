package main

type T struct {
	X int
}

func main() {
	var x = 1
	var p = &x
	*p = 2

	var t = &T{X: 1}
	_ = t
}
