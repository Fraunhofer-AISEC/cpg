package main

import "log"

func rangeLoop(params []int) {
	for _, p := range params {
		log.Printf("use %d", p)
	}
	log.Printf("after")
}

func indexLoop() {
	for i := 0; i < 3; i++ {
		log.Printf("in")
	}
	log.Printf("after")
}
