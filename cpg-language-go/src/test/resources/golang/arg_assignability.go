package main

// --- case 1: defined type -> unnamed underlying (valid Go) ---
type ObjectIdentifier []int

func marshalOID(oid []int) int { return len(oid) }

func callNamedToUnnamed() int {
	var oid ObjectIdentifier = []int{1, 2}
	return marshalOID(oid)
}

// --- case 2: unnamed -> defined type (valid Go) ---
type String []byte

func forEachSAN(der String) int { return len(der) }

func callUnnamedToNamed() int {
	b := []byte{1, 2}
	return forEachSAN(b)
}

// --- case 3: type ALIAS is the very same type (valid Go) ---
type chainVector = [8]uint32

func hashF(chain *[8]uint32) int { return int(chain[0]) }

func callThroughAlias() int {
	var cv chainVector
	return hashF(&cv)
}

// --- case 4: implementer -> interface (already worked) ---
type CheckWriteHook interface{ OnWrite() }
type CheckWriteAction uint8

func (a CheckWriteAction) OnWrite() {}

func terminalHookOverride(defaultHook, override CheckWriteHook) CheckWriteHook { return defaultHook }

func callImplementerToInterface() CheckWriteHook {
	var h CheckWriteHook
	return terminalHookOverride(CheckWriteAction(1), h)
}

func main() {
	callNamedToUnnamed()
	callUnnamedToNamed()
	callThroughAlias()
	callImplementerToInterface()
}
