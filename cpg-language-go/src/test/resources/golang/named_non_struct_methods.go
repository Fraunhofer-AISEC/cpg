package main

// ArrayMarshaler mirrors zapcore.ArrayMarshaler.
type ArrayMarshaler interface {
	MarshalLogArray(enc *Encoder) error
}

type Encoder struct{ n int }

// bools is a named *slice* type with a method, exactly as zap declares
// `type bools []bool`. It is not a struct, so it has no record declaration,
// yet it implements ArrayMarshaler.
type bools []bool

func (bs bools) MarshalLogArray(enc *Encoder) error {
	return appendBools(enc, bs)
}

func appendBools(enc *Encoder, bs bools) error { return nil }

// A named function type with a method, as in zapslog's handlerOptionFunc.
type optionFunc func(*Encoder)

func (f optionFunc) MarshalLogArray(enc *Encoder) error { return applyOption(f, enc) }

func applyOption(f optionFunc, enc *Encoder) error { return nil }

// A struct implementer, as a control: this one does work today.
type structImpl struct{}

func (s structImpl) MarshalLogArray(enc *Encoder) error { return nil }

func encode(m ArrayMarshaler, enc *Encoder) error {
	return m.MarshalLogArray(enc)
}

func main() {
	encode(bools{true}, &Encoder{})
}
