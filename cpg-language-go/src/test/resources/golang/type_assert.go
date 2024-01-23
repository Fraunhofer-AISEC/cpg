package main

import "fmt"

type MyStruct struct{}

type MyInterface interface {
	MyFunc()
}

func (MyStruct) MyFunc() {}

func main() {
	var f MyInterface = MyStruct{}
	var s = f.(MyStruct)

	fmt.Printf("%+v", s)

	var _ = MyInterface(s)
	var _ = interface{}(s)
	var _ = any(s)

	switch v := f.(type) {
	case MyStruct:
		var s2 = v
		fmt.Printf("%+v", s2)
	case *MyStruct:
		var p2 = v
		fmt.Printf("%+v", p2)
	default:
		var v2 = v
		fmt.Printf("%+v", v2)
	}
}

type myStruct struct{}

var test = (*myStruct)(nil)
