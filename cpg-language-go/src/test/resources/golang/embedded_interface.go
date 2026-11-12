package main

// PrimitiveArrayEncoder declares the methods, mirroring zapcore.
type PrimitiveArrayEncoder interface {
	AppendInt(v int)
	AppendString(s string)
}

// ArrayEncoder embeds it and adds its own, exactly as zapcore.ArrayEncoder
// embeds zapcore.PrimitiveArrayEncoder.
type ArrayEncoder interface {
	PrimitiveArrayEncoder

	AppendArray(m Marshaler) error
}

type Marshaler interface {
	MarshalLogArray(enc ArrayEncoder) error
}

// jsonEncoder implements the whole set structurally.
type jsonEncoder struct{ buf []byte }

func (j *jsonEncoder) AppendInt(v int)             { j.buf = appendInt(j.buf, v) }
func (j *jsonEncoder) AppendString(s string)       { j.buf = appendString(j.buf, s) }
func (j *jsonEncoder) AppendArray(m Marshaler) error { return m.MarshalLogArray(j) }

func appendInt(b []byte, v int) []byte       { return b }
func appendString(b []byte, s string) []byte { return b }

// ints is a named slice type calling a method that ArrayEncoder only has
// through the embedded interface -- this is zap's array.go.
type ints []int

func (nums ints) MarshalLogArray(arr ArrayEncoder) error {
	for i := range nums {
		arr.AppendInt(nums[i])
	}
	return nil
}

func main() {
	var e ArrayEncoder = &jsonEncoder{}
	ints{1}.MarshalLogArray(e)
}
