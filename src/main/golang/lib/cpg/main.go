package main

import (
	"cpg"
	"cpg/frontend"
	"go/parser"
	"go/token"

	"log"
	"unsafe"

	"tekao.net/jnigi"
)

//#include <jni.h>
import "C"

func main() {

}

//export Java_de_fraunhofer_aisec_cpg_frontends_golang_GoLanguageFrontend_parseInternal
func Java_de_fraunhofer_aisec_cpg_frontends_golang_GoLanguageFrontend_parseInternal(envPointer *C.JNIEnv, thisPtr C.jobject, arg1 C.jobject, arg2 C.jobject) C.jobject {
	env := jnigi.WrapEnv(unsafe.Pointer(envPointer))

	goFrontend := &frontend.GoLanguageFrontend{jnigi.WrapJObject(uintptr(thisPtr), "de/fraunhofer/aisec/cpg/frontends/golang/GoLanguageFrontend", false), nil}

	srcObject := jnigi.WrapJObject(uintptr(arg1), "java/lang/String", false)
	pathObject := jnigi.WrapJObject(uintptr(arg2), "java/lang/String", false)

	frontend.InitEnv(env)
	cpg.InitEnv(env)

	src, err := srcObject.CallMethod(env, "getBytes", jnigi.Byte|jnigi.Array)
	if err != nil {
		log.Fatal(err)
	}

	path, err := pathObject.CallMethod(env, "getBytes", jnigi.Byte|jnigi.Array)
	if err != nil {
		log.Fatal(err)
	}

	fset := token.NewFileSet()
	file, err := parser.ParseFile(fset, string(path.([]byte)), string(src.([]byte)), 0)
	if err != nil {
		log.Fatal(err)
	}

	goFrontend.File = file

	tu, err := goFrontend.HandleFile(fset, file, string(path.([]byte)))
	if err != nil {
		log.Fatal(err)

	}

	return C.jobject((*jnigi.ObjectRef)(tu).JObject())
}
