def foo(arg):
    return 7

def listComp(x, y):
    a = [foo(i) for i in x if i == 10]
    b = [foo(i) for i in x]
    c = {foo(i) for i in x if i == 10 if i < 20}
    d = [foo(i) for z in y if z in x for i in z if i == 10 ]
    foo(i)

def setComp(x, y):
    a = {foo(i) for i in x if i == 10}
    b = {foo(i) for i in x}
    c = {foo(i) for i in x if i == 10 if i < 20}
    d = {foo(i) for z in y if z in x for i in z if i == 10 }

def dictComp(x, y):
    a = {i: foo(i) for i in x if i == 10}
    b = {i: foo(i) for i in x}
    c = {i: foo(i) for i in x if i == 10 if i < 20}
    d = {i: foo(i) for z in y if z in x for i in z if i == 10 }

def generator(x, y):
    a = (i**2 for i in range(10) if i == 10)
    b = (i**2 for i in range(10))

def bar(k, v):
    return k+v

def tupleComp(x):
    a = [bar(k, v) for (k, v) in x]