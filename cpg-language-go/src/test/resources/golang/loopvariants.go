package main

import "log"

// A range loop: the call after the loop must be transferred once.
func rangePlain(params []int) {
	for _, p := range params {
		log.Printf("use %d", p)
	}
	log.Printf("after")
}

// A three-clause for loop.
func forIndex() {
	for i := 0; i < 3; i++ {
		log.Printf("in")
	}
	log.Printf("after")
}

// A range loop with continue, which adds a second back edge.
func rangeContinue(params []int) {
	for _, p := range params {
		if p > 1 {
			continue
		}
		log.Printf("use %d", p)
	}
	log.Printf("after")
}

// A range loop that always breaks, so it is no cycle in the EOG.
func rangeBreak(params []int) {
	for _, p := range params {
		log.Printf("use %d", p)
		break
	}
	log.Printf("after")
}

// An if/else: the merge after it must wait for both branches.
func ifElse(x int) {
	if x > 1 {
		log.Printf("a %d", x)
	} else {
		log.Printf("b")
	}
	log.Printf("after")
}

// A conditional break: both the normal exit and the break leave the loop.
func rangeBreakCond(params []int) {
	for _, p := range params {
		if p > 1 {
			break
		}
		log.Printf("use %d", p)
	}
	log.Printf("after")
}

// A conditional break with a tagged call before it, which leaves the loop with the break.
func rangeBreakCondTagged(params []int) {
	for _, p := range params {
		if p > 1 {
			log.Printf("stop %d", p)
			break
		}
		log.Printf("use %d", p)
	}
	log.Printf("after")
}

// A return inside the loop leaves the function, not only the loop.
func rangeReturn(params []int) {
	for _, p := range params {
		if p > 1 {
			log.Printf("ret %d", p)
			return
		}
		log.Printf("use %d", p)
	}
	log.Printf("after")
}

// Nested loops with a tagged call between the inner and the outer loop.
func nestedLoops(params [][]int) {
	for _, outer := range params {
		log.Printf("outer")
		for _, inner := range outer {
			log.Printf("inner %d", inner)
		}
	}
	log.Printf("after")
}

// A switch with three cases, so the merge after it has three predecessors.
func switchThreeCases(x int) {
	switch x {
	case 1:
		log.Printf("one %d", x)
	case 2:
		log.Printf("two %d", x)
	default:
		log.Printf("other %d", x)
	}
	log.Printf("after")
}

// An if without else: one predecessor of the merge does not change the state.
func ifNoElse(x int) {
	if x > 1 {
		log.Printf("big %d", x)
	}
	log.Printf("after")
}

// An if/else inside a loop body: a merge inside a loop.
func ifElseInLoop(params []int) {
	for _, p := range params {
		if p > 1 {
			log.Printf("big %d", p)
		} else {
			log.Printf("small %d", p)
		}
	}
	log.Printf("after")
}

// Nested if/else: the inner merge is a predecessor of the outer one.
func nestedIfElse(x int, y int) {
	if x > 1 {
		if y > 1 {
			log.Printf("both %d %d", x, y)
		} else {
			log.Printf("xOnly %d %d", x, y)
		}
	} else {
		log.Printf("neither %d %d", x, y)
	}
	log.Printf("after")
}
