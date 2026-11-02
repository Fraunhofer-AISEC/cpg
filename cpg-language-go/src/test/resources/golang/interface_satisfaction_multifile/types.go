package main

// EType is satisfied implicitly, as in gokrb5's crypto/etype.EType.
type EType interface {
	EncryptMessage(key []byte, message []byte) []byte
	GetETypeID() int32
}

// Aes256 declares no methods here. They live in methods.go, i.e. in a different
// translation unit than the type itself. This mirrors the common Go layout
// (e.g. mkcert declares `type mkcert struct` in main.go and its methods in
// cert.go) and exercises the cross-translation-unit lookup in
// DeclarationHandler.handleFuncDecl.
type Aes256 struct{}

// NotAnEType provides only part of the method set and must not be treated as an
// implementation of EType.
type NotAnEType struct{}
