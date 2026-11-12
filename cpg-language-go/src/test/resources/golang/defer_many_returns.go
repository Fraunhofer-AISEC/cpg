package main

import "os"

func work(a, b, c int) int {
	defer cleanup()

	// A defer followed by a chain of conditionals. The number of *paths* from the
	// defer to the returns grows exponentially with the number of branches, while
	// the number of reachable returns stays linear.
	if a > 0 {
		if b > 0 {
			return 1
		}
		return 2
	}
	if b > 0 {
		if c > 0 {
			return 3
		}
		return 4
	}
	if c > 0 {
		return 5
	}
	if len(os.Args) > 1 {
		return 6
	}
	return 7
}

func cleanup() {}

func main() { work(1, 2, 3) }
