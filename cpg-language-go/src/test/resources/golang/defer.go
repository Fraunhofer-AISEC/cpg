package p

import (
	"fmt"
	"os"
)

type MyStruct struct{}

func (s MyStruct) Do() {
	i := 1
	do()
	defer that(i)

	if len(os.Args) == 2 {
		i++
		return
	}

	i++
	fmt.Println("Still here, yay!")
}

func do() {

}

func that(i int) {
	fmt.Println(i)
}
