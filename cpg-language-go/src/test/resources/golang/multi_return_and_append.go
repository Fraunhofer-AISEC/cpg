package main

type Note struct{ Text string }

func open(body []byte) (*Note, error) { return &Note{}, nil }
func parseCheckpoint(text string) int { return len(text) }
func keyHash(name string, key []byte) uint32 { return uint32(len(name) + len(key)) }

// Case 1: multi-value := binding, then a field access on the bound variable.
func multiReturn(body []byte) int {
	n, err := open(body)
	if err != nil {
		return 0
	}
	return parseCheckpoint(n.Text)
}

// Case 2: the same, but bound inside an if/else-if chain as ct_monitor does.
func multiReturnInIf(body []byte) int {
	if n, err := open(body); err != nil {
		return 0
	} else {
		return parseCheckpoint(n.Text)
	}
}

// Case 3: the append builtin's result passed as an argument.
func appendResult(name string, keyID []byte) uint32 {
	return keyHash(name, append([]byte{0x05}, keyID...))
}

func main() { multiReturn(nil); multiReturnInIf(nil); appendResult("n", nil) }
