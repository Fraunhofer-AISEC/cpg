package p

import "fmt"

func main() {
    myTest("some string")
}

func myTest(s string) (err error) {
    fmt.Printf("%s", s)

    a := 1 + 2

    err = nil

    return
}
