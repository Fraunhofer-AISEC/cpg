package main

type Tree struct{ name string }

// A package-level function named Parse ...
func Parse(name, text, left, right string, funcs ...map[string]interface{}) (map[string]*Tree, error) {
	set := make(map[string]*Tree)
	t := New(name)
	_, err := t.Parse(text, left, right, set, funcs...)
	return set, err
}

func New(name string) *Tree { return &Tree{name: name} }

// ... and a *method* with the same local name but a different signature.
func (t *Tree) Parse(text, left, right string, set map[string]*Tree, funcs ...map[string]interface{}) (*Tree, error) {
	return t, reachedOnlyViaMethod()
}

func reachedOnlyViaMethod() error { return nil }

func main() { Parse("n", "t", "{{", "}}") }
