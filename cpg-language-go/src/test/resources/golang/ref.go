package main

import (
    "auth"
)

var service *auth.Service

func main() {
    service = &auth.Service{Name: "MyName"}
}
