array1_size = 16
array1 = bytearray(16)
array2 = bytearray(256 * 512)
temp = 0


def _mm_lfence():
    pass


def victim_function(x):
    global temp
    if x < array1_size:
        temp &= array2[array1[x] * 512]


def victim_function_split(x):
    global temp
    if x < array1_size:
        v = array1[x]
        temp &= array2[v * 512]


def victim_function_fenced(x):
    global temp
    if x < array1_size:
        _mm_lfence()
        temp &= array2[array1[x] * 512]


def safe_function(x):
    if x < array1_size:
        return array1[x]
    return 0
