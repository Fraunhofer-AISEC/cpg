package main

func pair(a int, b int) int {
	return a + b
}

func main() {
	// Not valid Go: one argument too few. Go has no overloading, so the name still denotes pair.
	_ = pair(1)
}
