package main

import "example.com/shadow/liner"

func main() {
	// The variable shadows the package name from here on.
	liner := liner.NewLiner()
	defer liner.Close()
	liner.AppendHistory("hello")
}
