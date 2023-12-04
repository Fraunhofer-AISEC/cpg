package p

import (
	fmtother "fmt"
)

type formatter struct {
	field int
}

func main() {
	fmt := formatter{}
	fmt.field = 1

	fmtother.Printf("%d", 1)
}
