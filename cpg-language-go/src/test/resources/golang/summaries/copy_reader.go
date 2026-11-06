package main

import (
	"bytes"
	"io"
)

// Without the standard library, io.Reader is the type of an inferred record.
func copyFromReader(r io.Reader) {
	var body bytes.Buffer
	io.Copy(&body, r)
	afterCopy := body
	_ = afterCopy
}
