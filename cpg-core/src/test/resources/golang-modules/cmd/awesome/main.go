package main

import (
	"example.io/awesome"
	"example.io/awesome/util"
)

func main() {
	a := awesome.NewAwesome()

	util.DoSomethingWith(a)
	util.DoSomethingElse() // this function is part of a file we do not have
}
