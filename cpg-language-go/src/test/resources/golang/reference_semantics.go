package main

type Source interface {
	Read(p []byte) (int, error)
}

type Handler func(int)

type Counts map[string]int

type Events chan string

type Point struct {
	X int
}

func references(m map[string]int, c chan int, f func(int), a any, e error, anon interface{ Close() error }, s Source, h Handler, counts Counts, events Events) {
}

func values(i int, t string, p Point) {
}
