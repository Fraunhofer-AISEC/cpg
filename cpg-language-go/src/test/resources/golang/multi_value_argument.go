package main

type Pos int
type PipeNode struct{ p Pos }
type ListNode struct{ p Pos }
type IfNode struct{ p Pos }

// Returns five values.
func parseControl(allowElse bool, context string) (Pos, int, *PipeNode, *ListNode, *ListNode) {
	return 0, 0, nil, nil, nil
}

func newIf(pos Pos, line int, pipe *PipeNode, list, elseList *ListNode) *IfNode {
	return &IfNode{p: reached(pos)}
}

func reached(p Pos) Pos { return p }

// Go binds the five results of parseControl to newIf's five parameters.
func ifControl() *IfNode { return newIf(parseControl(true, "if")) }

// Two results into a two-parameter function.
func two() (int, string)          { return 1, "a" }
func takesTwo(i int, s string) int { return i + len(s) }
func useTwo() int                  { return takesTwo(two()) }

func main() { ifControl(); useTwo() }
