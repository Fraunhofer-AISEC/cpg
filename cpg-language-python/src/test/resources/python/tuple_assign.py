def returnTuple(a, b):
    return (a, b)

def getTuple(a, b):
    (c, d) = returnTuple(a, b)
    print(str(c) + " " + str(d))

def returnTuple2(a, b):
    return a, b

def getTuple2(a, b):
    c, d = returnTuple2(a, b)
    print(str(c) + " " + str(d))

def getTuple3(a, b):
    (c, d) = returnTuple2(a, b)
    print(str(c) + " " + str(d))

def getTuple4(a, b):
    c, d = returnTuple(a, b)
    print(str(c) + " " + str(d))