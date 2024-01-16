package p

type myError string

func (err myError) Error() string {
	return string(err)
}

var s = error(myError("abc"))
