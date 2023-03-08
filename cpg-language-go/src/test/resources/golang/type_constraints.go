package main

type MyStruct[T any] {}
type MyInterface{}

func main() {
    _ := &MyStruct[MyInterface]{}
}