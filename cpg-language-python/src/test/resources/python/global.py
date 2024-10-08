c = 1

def global_write():
    global c
    # this writes to a global variable c
    c = 2
    # this reads from the global variable c
    print(c)

def global_read():
    print(c)

def local_write():
    # this writes to a local variable c
    c = 3
    # this reads from the local variable c
    print(c)

def error_write():
    # this should result in an error/unresolved c variable;
    # in the CPG this will probably still resolve to our local c
    # because we do not take the EOG into account when resolving
    # symbols (yet)
    c = c + 1

global_write()
local_write()
error_write()