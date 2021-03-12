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
func Java_de_fraunhofer_aisec_cpg_frontends_golang_GoLanguageFrontend_parseInternal(envPointer *C.JNIEnv, thisPtr C.jobject, arg1 C.jobject) C.jobject {
	//func Java_de_fraunhofer_aisec_cpg_JNITest_parse(envPointer *C.JNIEnv, clazz C.jclass, arg1 C.jobject) C.jobject {
	env := jnigi.WrapEnv(unsafe.Pointer(envPointer))

	goFrontend := (*frontend.GoLanguageFrontend)(jnigi.WrapJObject(uintptr(thisPtr), "de/fraunhofer/aisec/cpg/frontends/golang/GoLanguageFrontend", false))

	srcObject := jnigi.WrapJObject(uintptr(arg1), "java/lang/String", false)

	frontend.InitEnv(env)
	cpg.InitEnv(env)

	src, err := srcObject.CallMethod(env, "getBytes", jnigi.Byte|jnigi.Array)
	if err != nil {
		log.Fatal(err)
	}

	fset := token.NewFileSet()
	file, err := parser.ParseFile(fset, "src.go", string(src.([]byte)), 0)
	if err != nil {
		panic(err)
	}

	tu, err := goFrontend.HandleFile(fset, file)
	if err != nil {
		panic(err)
	}

	return C.jobject((*jnigi.ObjectRef)(tu).JObject())
}
