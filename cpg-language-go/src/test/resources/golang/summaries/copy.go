package main

import (
	"bytes"
	"io"
)

// A declared interface, so its type is known.
type Source interface {
	Read(p []byte) (int, error)
}

func copyFromSource(s Source) {
	var body bytes.Buffer
	io.Copy(&body, s)
	afterCopy := body
	_ = afterCopy
}

func copyFromPointer(src *bytes.Buffer) {
	var body bytes.Buffer
	io.Copy(&body, src)
	afterCopy := body
	_ = afterCopy
}
