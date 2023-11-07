package p

import (
	"fmt"
)

type OtherStruct struct {
}

type EvenAnotherStruct struct {
}

type MyStruct struct {
	MyField int
	OtherStruct
	*EvenAnotherStruct
}

type MyInterface interface {
	MyFunc() string
}

func (s MyStruct) MyFunc() string {
	fmt.Printf(s.myOtherFunc(1), s.MyField)

	return "s"
}

func (s MyStruct) myOtherFunc(i int) string {
	return fmt.Sprintf("%d", i)
}

func NewMyStruct() *MyStruct {
	return &MyStruct{}
}

var s = struct {
	field int
}{
	field: 1,
}

func DoInterface(i MyInterface) string {
	return i.MyFunc()
}

func main() {
	var myStruct = NewMyStruct()
	DoInterface(myStruct)
}
