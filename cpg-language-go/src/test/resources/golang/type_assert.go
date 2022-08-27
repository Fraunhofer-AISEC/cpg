package main

import "fmt"

type MyStruct struct {}
type MyInterface interface {
    MyFunc()
}
func (MyStruct) MyFunc() {}

func main () {
    var f MyInterface = MyStruct{}
    var s = f.(MyStruct)

    fmt.Printf("%+v", s)
}