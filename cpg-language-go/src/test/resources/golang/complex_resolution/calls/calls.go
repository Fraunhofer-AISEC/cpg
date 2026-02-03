package calls

import "mymodule.io/complex_resolution/util"

type Something struct {
	self  *Something
	funcy Funcy
}

func (s *Something) Do() {
	Func()
}

func NewSomething() *Something {
	return &Something{}
}
func main() {
	var some = NewSomething()
	some.funcy = do

	some.self.self.self.Do()

	// A function as parameter
	_ = doFuncy(func() error {
		return nil
	})

	// Passing "nil" as a function type
	_ = doFuncy(nil)

	// Passing "nil" as an interface
	doSomething(nil)

	// Passing "nil" as pointer
	doPointer(nil)

	// Numeric literals can be assigned to a function accepting of any numeric type
	doInt64(1)

	// Numeric literals can also be used in simple arithmetic of the underlying type is numeric
	doLength(5 * util.Meter)

	// interface{} and any should be interchangeable
	var a any = nil
	old(a)

	_ = doFuncy(some.funcy)
}

func Func(args ...int) {}

type Funcy func() error

type Somethinger interface {
	Something() (*t, error)
}

type s struct {
	*inner
}

type t struct {
	s *s
}

func (s *s) Do() {}

type inner struct{}

func (i *inner) DoInner() {}

func doFuncy(funcy Funcy) (err error) {
	err = funcy()
	return
}

func doSomething(s Somethinger) {
	t, err := s.Something()
	_ = err

	t.s.Do()
	t.s.DoInner()
}

func doPointer(p *int) {}

func doInt64(i int64) {}

func doLength(l util.Length) {}

func old(v interface{}) {}

func do() error {
	return nil
}
