package main

import (
	"os"
)

func main() {
	x := "G"
	y := "T"
	z := "P"

	var f string
	if len(os.Args) == 2 {
		f = x + y + z
	} else {
		f = x + z + y
	}

	_ = f
}
