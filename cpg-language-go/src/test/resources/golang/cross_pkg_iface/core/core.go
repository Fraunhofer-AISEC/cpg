package core

// ArrayMarshaler mirrors zapcore.ArrayMarshaler: declared in one package,
// implemented in another.
type ArrayMarshaler interface {
	MarshalLogArray(enc Encoder) error
}

type Encoder interface {
	AppendBool(b bool)
}

type jsonEncoder struct{}

func (j *jsonEncoder) AppendBool(b bool) {}

// samePkgImpl implements the interface from inside the declaring package.
type samePkgImpl struct{}

func (s samePkgImpl) MarshalLogArray(enc Encoder) error { return nil }

// Encode dispatches through the interface, as zapcore's json_encoder does.
func Encode(m ArrayMarshaler) error {
	return m.MarshalLogArray(&jsonEncoder{})
}
