package p

import ("http")

func main() {
    c := NewMyStruct()
	c.myOtherFunc()

	go c.MyFunc()
	go c.MyFunc()
}
