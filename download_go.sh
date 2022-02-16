#!/bin/bash
GO_VERSION=1.17.7

wget https://go.dev/dl/go${GO_VERSION}.linux-amd64.tar.gz
tar -C ~/ -xzf go${GO_VERSION}.linux-amd64.tar.gz
