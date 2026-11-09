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

// Aes192, like Aes256, declares its methods in methods.go. One of them uses an
// *unnamed* receiver, which Go permits when the method does not touch the
// receiver. Such a method has no receiver variable, and because the type is
// declared in a different translation unit it is not bound to its record
// either, so it carries neither of the obvious keys back to Aes192 -- yet it is
// part of the method set, and Aes192 does satisfy EType. This is the shape of
// miekg/dns, which declares `type OPT struct` in edns.go, `func (*OPT) parse`
// with an unnamed receiver, and `func (rr *OPT) pack` in generated zmsg.go.
type Aes192 struct{}

// NotAnEType provides only part of the method set and must not be treated as an
// implementation of EType.
type NotAnEType struct{}
