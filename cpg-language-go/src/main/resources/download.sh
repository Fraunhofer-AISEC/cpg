#!/bin/bash
VERSION=v0.0.2

curl https://github.com/Fraunhofer-AISEC/libgoast/releases/download/${VERSION}/libgoast-amd64.dylib -L -s -o libgoast-amd64.dylib
curl https://github.com/Fraunhofer-AISEC/libgoast/releases/download/${VERSION}/libgoast-arm64.dylib -L -s -o libgoast-arm64.dylib
curl https://github.com/Fraunhofer-AISEC/libgoast/releases/download/${VERSION}/libgoast-amd64.so -L -s -o libgoast-amd64.so
