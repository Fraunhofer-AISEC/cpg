#!/bin/bash
ARCH=`uname -s | tr '[:upper:]' '[:lower:]'`

if [ $ARCH == "darwin" ]
then
    EXTENSION="dylib"
else
    EXTENSION="so"
fi

if [ "$JAVA_HOME" == "" ]
then
    JAVA_HOME=`/usr/libexec/java_home`
fi

export CGO_CFLAGS="-I${JAVA_HOME}/include -I/${JAVA_HOME}/include/${ARCH}"

go build -buildmode=c-shared -o ../resources/libcpgo.${EXTENSION} lib/cpg/main.go
