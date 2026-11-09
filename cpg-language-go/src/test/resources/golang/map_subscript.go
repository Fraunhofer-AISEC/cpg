package main

type Value interface {
	Set(s string) error
}

type Flag struct {
	Name  string
	Value Value
}

type FlagSet struct {
	formal map[string]*Flag
}

// Set mirrors spf13/pflag: the flag is obtained from a map with a comma-ok
// lookup and then dispatched on. If the subscript has no type, `flag` is
// untyped, `flag.Value` is untyped, and `flag.Value.Set` cannot be dispatched.
func (f *FlagSet) Set(name, value string) error {
	flag, ok := f.formal[name]
	if !ok {
		return nil
	}
	return flag.Value.Set(value)
}

// plain indexing, without the comma-ok form
func (f *FlagSet) lookup(name string) *Flag {
	return f.formal[name]
}

// iterating a map must still yield both the key and the value
func (f *FlagSet) names() []string {
	var out []string
	for k, v := range f.formal {
		if v != nil {
			out = append(out, k)
		}
	}
	return out
}

type stringValue struct{}

func (s *stringValue) Set(v string) error { return nil }

func main() {
	fs := &FlagSet{formal: map[string]*Flag{}}
	fs.formal["x"] = &Flag{Name: "x", Value: &stringValue{}}
	fs.Set("x", "y")
	fs.lookup("x")
	fs.names()
}
