package spectre

var array1Size uint = 16
var array1 [16]uint8
var array2 [256 * 512]uint8
var temp uint8 = 0

func _mm_lfence() {}

func victim_function(x uint) {
	if x < array1Size {
		temp &= array2[uint(array1[x])*512]
	}
}

func victim_function_split(x uint) {
	if x < array1Size {
		v := array1[x]
		temp &= array2[uint(v)*512]
	}
}

func victim_function_fenced(x uint) {
	if x < array1Size {
		_mm_lfence()
		temp &= array2[uint(array1[x])*512]
	}
}

func safe_function(x uint) uint8 {
	if x < array1Size {
		return array1[x]
	}
	return 0
}
