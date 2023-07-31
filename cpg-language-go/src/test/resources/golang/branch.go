package p

import "fmt"

func main() {
	i := 0

start:
	for {
		switch i {
		case 0:
			continue
		case 1:
			break start // will break out of the switch and the for-loop
		case 2:
			fallthrough // will fall through case 3
		case 3:
			fmt.Printf("%d", i)
		default:
			goto end
		}
		i++
	}

end:
}
