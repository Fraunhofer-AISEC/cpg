package main

// APIError mirrors the shape of certmagic's zerossl.APIError: a concrete type
// that satisfies the *builtin* `error` interface structurally, i.e. it never
// names `error` and does not embed it.
type APIError struct {
	Code int
}

func (e APIError) Error() string {
	return apiErrorText(e.Code)
}

func apiErrorText(code int) string {
	return "api error"
}

// WrappedError, by contrast, embeds `error` explicitly. Types of this shape are
// already linked to `error` through their super types, so they must keep
// working.
type WrappedError struct {
	error
}

// NotAnError only looks similar; `Error` has the wrong arity and must therefore
// not be treated as a dispatch target for `error`.
type NotAnError struct{}

func (n NotAnError) Error(verbose bool) string {
	return "not an error"
}

// report dispatches through the builtin interface. Without the implicit
// satisfaction relation for `error`, this resolves only to `error.Error`, which
// has no body, and the call graph stops here -- which is what loses the bulk of
// certmagic's reachable set.
func report(err error) string {
	return err.Error()
}

func main() {
	report(APIError{Code: 400})
}
