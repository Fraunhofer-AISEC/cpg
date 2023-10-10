package p

func main() {
	c := NewMyStruct()
	if i := len("a"); i < 1 {
		c.myOtherFunc(i)
	}

	go c.MyFunc()

	// In Go, numeric literals can be used as any numeric type
	sixtyfour(1)
}

func sixtyfour(i int64) {

}
