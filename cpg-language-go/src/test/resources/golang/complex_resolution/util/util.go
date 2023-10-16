package util

type Length int64

func (l Length) Centimeter() int64 {
	return int64(l)
}

const (
	Centimeter Length = 1
	Meter             = 100 * Centimeter
)
