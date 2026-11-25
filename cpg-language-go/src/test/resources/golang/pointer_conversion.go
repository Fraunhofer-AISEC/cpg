package main

import "unsafe"

type header struct {
	n int
}

func isNativeEndianLittle() bool {
	var x = 0x01234567
	var p = unsafe.Pointer(&x)
	// A conversion to a pointer-to-array type.
	var bp = (*[4]byte)(p)
	return bp[0] == 0x67
}

func asHeader(p unsafe.Pointer) int {
	// A conversion to a pointer-to-struct type.
	h := (*struct{ n int })(p)
	return h.n
}
