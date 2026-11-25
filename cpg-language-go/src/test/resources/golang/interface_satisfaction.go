package main

// EType mirrors the shape of gokrb5's crypto/etype.EType: an interface that is
// satisfied implicitly, i.e. no concrete type ever names it.
type EType interface {
	EncryptMessage(key []byte, message []byte) []byte
	GetETypeID() int32
}

type Aes256 struct{}

func (e Aes256) EncryptMessage(key []byte, message []byte) []byte {
	return aes256Encrypt(key, message)
}

func (e Aes256) GetETypeID() int32 {
	return 18
}

type Aes128 struct{}

func (e Aes128) EncryptMessage(key []byte, message []byte) []byte {
	return aes128Encrypt(key, message)
}

func (e Aes128) GetETypeID() int32 {
	return 17
}

// NotAnEType only provides part of the method set and must therefore not be
// considered a dispatch target for EType.
type NotAnEType struct{}

func (n NotAnEType) GetETypeID() int32 {
	return 0
}

func aes256Encrypt(key []byte, message []byte) []byte {
	return message
}

func aes128Encrypt(key []byte, message []byte) []byte {
	return message
}

// getEncryptedData dispatches through the interface, mirroring gokrb5's
// crypto.GetEncryptedData. Without the implicit interface satisfaction relation
// this call resolves only to EType.EncryptMessage, which has no body, and the
// call graph stops here.
func getEncryptedData(e EType, key []byte, message []byte) []byte {
	return e.EncryptMessage(key, message)
}

func main() {
	var e EType = Aes256{}
	getEncryptedData(e, []byte("key"), []byte("message"))
}
