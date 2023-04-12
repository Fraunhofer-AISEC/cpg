package p

import "fmt"

func main() {
	var i int
	var err error

	i, err = myTest("some string")

	if err == nil {
		fmt.Printf("%d", i)
	}
}

func myTest(s string) (a int, err error) {
	fmt.Printf("%s", s)

	a = 1 + 2

	err = nil

	return
}
