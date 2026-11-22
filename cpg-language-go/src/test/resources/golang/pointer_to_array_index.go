package main

// g takes plain uint32 values, as blake3's compress_pure.g does.
func g(a, b, c, d uint32) (uint32, uint32) {
	return a + b, c + d
}

// rcompress indexes through *pointers to arrays*. Go auto-dereferences the
// pointer when indexing, so s[0] has type uint32, not [16]uint32. If the
// subscript keeps the array type, the arguments no longer match g's parameters
// and the call resolves to an inferred stub instead of the real function.
func rcompress(s *[16]uint32, m *[16]uint32) uint32 {
	s0, s1 := s[0], s[1]
	x, y := g(s0, s1, m[0], m[1])
	return x + y
}

func main() {
	var s, m [16]uint32
	rcompress(&s, &m)
}
