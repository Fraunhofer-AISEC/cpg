package main

import "fmt"

func main() {
    var bytes = []byte{1,2,3,4}

    // Regular old-school for loop
    for i := 0; i < 4; i++ {
        fmt.Printf("bytes[%d]=%d\n", i, bytes[i])
    }

    // For-each style loop with range expression with key and value. idx and b are created using
    // the short assignment syntax. Its scope is limited to the for-block.
    for idx, b := range bytes {
        fmt.Printf("bytes[%d]=%d; idx=%T b=%T\n", idx, b, idx, b)
    }
}