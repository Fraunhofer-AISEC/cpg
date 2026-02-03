package p

import "otherPackage"

type Receiver struct {
	Field int
}

// For now this must be specified AFTER our type declaration, because otherwise we wont find it
func (r Receiver) myFunc() {
	r.Field = otherPackage.OtherField
}
