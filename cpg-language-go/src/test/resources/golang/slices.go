package main

import "fmt"

func main() {
    a := []int{1,2,3}

    // [1]
    b := a[:1]

    // [2, 3]
    c := a[1:]

    // [1]
    d := a[0:1]

    // [1]
    e := a[0:1:1]

    fmt.Printf("%v %v %v %v %v", a, b, c, d, e)
}