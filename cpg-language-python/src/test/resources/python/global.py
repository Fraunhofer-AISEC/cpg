c = 1

def global_write():
    global c
    # this writes to a global variable c
    c = 2
    print(c)

def local_write():
    # this writes to a local variable c
    c = 3
    print(c)

global_write()
local_write()
