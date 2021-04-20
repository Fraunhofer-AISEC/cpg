package p

import ("fmt")

type MyStruct struct {
    MyField int
}

type MyInterface interface {
	MyFunc() string
}

func (s MyStruct) MyFunc() string {
    fmt.Printf(s.myOtherFunc(), s.MyField)

	return "s"
}

func (s MyStruct) myOtherFunc() string {
	return "%d"
}

func NewMyStruct() *MyStruct {
    return &MyStruct{}
}
