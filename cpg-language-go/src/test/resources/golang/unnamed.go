package main

import "context"

type MyStruct struct{}

func (MyStruct) MyFunc() int {
	return 1
}

func MyGlobalFunc(_ context.Context) int {
	return 2
}
