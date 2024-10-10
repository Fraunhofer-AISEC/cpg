def foo(arg):
    return 7

def listComp(x, y):
    a = [foo(i) for i in x if i == 10]
    b = [foo(i) for i in x]
    d = [foo(i) for z in y if z in x for i in z if i == 10 ]

def setComp(x, y):
    a = {foo(i) for i in x if i == 10}
    b = {foo(i) for i in x}
    d = {foo(i) for z in y if z in x for i in z if i == 10 }

def dictComp(x, y):
    a = {i: foo(i) for i in x if i == 10}
    b = {i: foo(i) for i in x}
    d = {i: foo(i) for z in y if z in x for i in z if i == 10 }