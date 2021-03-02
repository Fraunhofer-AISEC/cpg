package p

import "fmt"

func main() {
    myTest("some string")
}

func myTest(s string) (err error) {
    fmt.Printf("%s", s)

    return nil
}
