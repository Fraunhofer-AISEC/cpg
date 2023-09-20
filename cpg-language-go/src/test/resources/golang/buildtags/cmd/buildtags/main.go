package main

import (
	"fmt"

	"mymodule.io/buildtags"
)

func main() {
	fmt.Printf("Your OS: %s", buildtags.OS())
}
