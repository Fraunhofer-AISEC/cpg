package p

import "fmt"

func main() {
	var (
		i   int
		err error
	)

	i, err = funcA("some string")

	if err == nil {
		fmt.Printf("%d", i)
	}
}

func funcA(s string) (a int, err error) {
	fmt.Printf("%s", s)

	a = 1 + 2

	err = nil

	return
}

// funcB is a function that show-cases different ways of declaring used and unused
// parameters in Go.
func funcB(a, b []byte, _ int) (byte, byte) {
	return a[0], b[0]
}

func funcC(string) {
	return
}
