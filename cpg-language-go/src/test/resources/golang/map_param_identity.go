package main

type Tree struct{ name string }

// The parameter's map type is written in this function's scope ...
func (t *Tree) Parse(text string, set map[string]*Tree, funcs ...map[string]interface{}) int {
	return len(set)
}

// ... while the argument's map type is written in a different scope. They are
// the same Go type and the call must resolve.
func Parse(name, text string, funcs ...map[string]interface{}) int {
	set := make(map[string]*Tree)
	t := &Tree{name: name}
	return t.Parse(text, set, funcs...)
}

// A map passed to a plain function, again from another scope.
func countKeys(m map[string]int) int { return len(m) }

func useCountKeys() int {
	m := map[string]int{"a": 1}
	return countKeys(m)
}

func main() { Parse("n", "t"); useCountKeys() }
