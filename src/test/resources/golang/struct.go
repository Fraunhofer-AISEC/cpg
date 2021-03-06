package p

type MyStruct struct {
    MyField int
}

type MyInterface interface {
	MyFunc() string
}

func (s MyStruct) MyFunc() string {
	return "s"
}
