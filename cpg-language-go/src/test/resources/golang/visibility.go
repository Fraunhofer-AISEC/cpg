package p

// Exported (upper-case) top-level function -> Visibility.PUBLIC
func ExportedFunc() {}

// unexported (lower-case) top-level function -> Visibility.PACKAGE
func unexportedFunc() {}

// Exported and unexported top-level variables
var ExportedVar = 1
var unexportedVar = 2

// The blank identifier has no export semantics -> Visibility.UNKNOWN
var _ = 3

// Exported and unexported top-level constants
const ExportedConst = 1
const unexportedConst = 2

// Unicode: the export decision is made on the first *rune*.
var Über = 1
var über = 2

// Exported struct type with a mix of exported and unexported fields
type ExportedStruct struct {
	ExportedField   int
	unexportedField int
}

// unexported struct type
type unexportedStruct struct {
	Field int
}

// Exported method on ExportedStruct
func (s ExportedStruct) ExportedMethod() {}

// unexported method on ExportedStruct
func (s ExportedStruct) unexportedMethod() {}

// Exported interface with exported and unexported methods
type ExportedInterface interface {
	ExportedDo()
	unexportedDo()
}

// Exported and unexported package-level type aliases
type ExportedAlias = int
type unexportedAlias = int

func local() {
	// Local variables are block-scoped and carry no visibility restriction, even when
	// declared with a classic `var` (which reaches handleValueSpec) and an exported casing.
	notExported := 1
	_ = notExported

	var Exported = 1
	var unexported = 2
	_ = Exported
	_ = unexported
}
