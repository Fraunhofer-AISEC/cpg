package zap

import "example.com/cross_pkg_iface/core"

// bools implements core.ArrayMarshaler from a *different* package, exactly as
// go.uber.org/zap implements go.uber.org/zap/zapcore's interfaces.
type bools []bool

func (bs bools) MarshalLogArray(enc core.Encoder) error {
	return appendAll(enc, bs)
}

func appendAll(enc core.Encoder, bs bools) error { return nil }

func Run() error { return core.Encode(bools{true}) }
