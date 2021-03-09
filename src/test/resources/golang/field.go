package p

import "otherPackage"

func (r Receiver) myFunc() {
   r.Field = otherPackage.OtherField
}

type Receiver struct {
    Field int
}
