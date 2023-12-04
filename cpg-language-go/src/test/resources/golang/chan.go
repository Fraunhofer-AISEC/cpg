package pa

func main() {
	var v int
	var ch = make(chan int)
	ch <- v
	<-ch
}
