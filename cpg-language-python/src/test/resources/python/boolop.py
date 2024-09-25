def twoBoolOp(a):
    if a and True:
        print(a)
    return a

def threeBoolOp(a, b):
    if a and True and b:
        print(a)
    return a

def nestedBoolOpDifferentOp(a, b):
    if a and True or b:
        print(a)
    return a

def nestedBoolOpDifferentOp2(a, b):
    if a or True and b:
        print(a)
    return a

def threeBoolOpNoBool(a):
    if a and True and "foo":
        print(a)
    return a