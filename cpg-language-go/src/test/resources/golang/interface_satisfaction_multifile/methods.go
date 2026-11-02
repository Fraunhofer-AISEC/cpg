package main

// The receiver types are declared in types.go, so resolving them requires a
// lookup across translation units.

func (e Aes256) EncryptMessage(key []byte, message []byte) []byte {
	return aes256Encrypt(key, message)
}

func (e Aes256) GetETypeID() int32 {
	return 18
}

func (n NotAnEType) GetETypeID() int32 {
	return 0
}

func aes256Encrypt(key []byte, message []byte) []byte {
	return message
}

// getEncryptedData dispatches through the interface. Everything below it is
// reachable only via the implementation.
func getEncryptedData(e EType, key []byte, message []byte) []byte {
	return e.EncryptMessage(key, message)
}

func main() {
	var e EType = Aes256{}
	getEncryptedData(e, []byte("key"), []byte("message"))
}
