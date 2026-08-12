package p

type Node struct {
	next *Node
}

func main() {
	var head Node
	var n Node
	head.next = &n

	var target *Node
	var x **Node
	x = &target
	*x = head.next
}

