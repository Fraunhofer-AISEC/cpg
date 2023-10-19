package p

type custom int64

const (
	one custom = 1
	oneAsWell
	ten = 10
	tenAsWell
)

const (
	two = 2 + iota
	three
	four
)

const (
	five, fivehundred = 5 + iota, (5 + iota) * 100
	six, sixhundred
)

const (
	fiveAsWell = 5 + iota*100
	onehundredandfive
)

const (
	oneShift = 1 << iota
	twoShift
	zeroShift = 1 >> iota
	zeroAnd   = oneShift & twoShift
	threeOr   = oneShift | twoShift
	threeXor  = oneShift ^ twoShift
)
