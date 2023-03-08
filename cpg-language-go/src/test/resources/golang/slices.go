package main

import "fmt"

func main() {
    var a = []int{1,2,3}

    // [1]
    var b = a[:1]

    // [2, 3]
    var c = a[1:]

    // [1]
    var d = a[0:1]

    // [1]
    var e = a[0:1:1]

    fmt.Printf("%v %v %v %v %v", a, b, c, d, e)
}