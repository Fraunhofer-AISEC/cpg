package p

import (
	"fmt"
	"os"
)

func main() {
	do()
	defer that()

	if len(os.Args) == 2 {
		return
	}

	fmt.Println("Still here, yay!")
}

func do() {

}

func that() {

}
