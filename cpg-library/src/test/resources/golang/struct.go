package p

import ("fmt")

// Comment on a Struct
type MyStruct struct {
    MyField int
}

// Comment on an Interface
type MyInterface interface {
	MyFunc() string
}

func (s MyStruct) MyFunc() string {
    fmt.Printf(s.myOtherFunc(), s.MyField)

	return "s"
}

// Comment on a Method
func (s MyStruct) myOtherFunc() string {
	return "%d"
}

// Comment on a Function
func NewMyStruct() *MyStruct {
    return &MyStruct{}
}
