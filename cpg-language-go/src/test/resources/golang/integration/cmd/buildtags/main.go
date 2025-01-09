package main

import (
	"fmt"

	"mymodule.io/integration"
)

func main() {
	fmt.Printf("Your OS: %s", buildtags.OS())
}
