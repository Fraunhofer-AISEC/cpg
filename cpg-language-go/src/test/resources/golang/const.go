package p

const (
	one = 1
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
