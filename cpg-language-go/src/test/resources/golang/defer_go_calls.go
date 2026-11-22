package main

import (
	"io"
	"net"
)

func closeSilent(closeable io.Closer) {
	_ = closeable.Close()
}

func handle(conn net.Conn) {
	defer closeSilent(conn)
	conn.Write([]byte("hello"))
}

func main() {
	listener, _ := net.Listen("tcp", "0.0.0.0:8080")
	for {
		conn, _ := listener.Accept()
		go handle(conn)
	}
}
