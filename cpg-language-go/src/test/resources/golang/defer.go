package p

import (
	"fmt"
	"os"
)

func main() {
	i := 1
	do()
	defer that(i)

	if len(os.Args) == 2 {
		i++
		return
	}

	i++
	fmt.Println("Still here, yay!")
	return
}

func do() {

}

func that(i int) {
	fmt.Println(i)
}
