package chained

type Type interface {
	Elem() Type
}

func TypeOf(i any) Type {
	return nil
}

type MyStruct interface{}

var (
	structType = TypeOf((*MyStruct)(nil)).Elem()
	_          = structType
)
