package main

type MyStruct[T any] struct {}
type MyInterface interface {}

func SomeFunc[T any, S MyInterface]() {}

func main() {
    _ := &MyStruct[MyInterface]{}
    SomeFunc[any, MyInterface]()
}