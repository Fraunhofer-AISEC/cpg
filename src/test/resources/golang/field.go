package p

import "otherPackage"

func (r Receiver) myFunc() {
   r.Field = otherPackage.Field
}