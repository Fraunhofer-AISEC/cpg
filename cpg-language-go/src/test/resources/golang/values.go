package p

import "net/url"

func main() {
	name := "firstname lastname"
	data := "data"

	message := url.Values{
		"Name": []string{name},
		"Data": []string{data},
	}
}
