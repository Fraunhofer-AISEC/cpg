#!/bin/bash
ARCH=`uname -s | tr '[:upper:]' '[:lower:]'`

if [ $ARCH == "darwin" ]
then
    EXTENSION="dylib"
else
    EXTENSION="so"
fi

CGO_ENABLED=1 GOARCH=amd64 go build -buildmode=c-shared -o ../resources/libcpgo-amd64.${EXTENSION} lib/cpg/main.go

if [ $ARCH == "darwin" ]
then
    CGO_ENABLED=1 GOARCH=arm64 go build -buildmode=c-shared -o ../resources/libcpgo-arm64.${EXTENSION} lib/cpg/main.go
fi
