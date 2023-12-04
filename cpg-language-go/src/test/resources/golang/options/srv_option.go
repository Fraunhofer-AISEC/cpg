package options

type Option func(*srv)

func WithField(a int) Option {
	return func(s *srv) {
		s.inner.field = a
	}
}
