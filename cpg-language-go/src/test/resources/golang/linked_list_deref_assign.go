package p

type Node struct {
	next *Node
}

func main() {
	var head *Node
	var n Node
	var y *Node
	y = &n

	var x **Node
	x = &head
	*x = y
}

