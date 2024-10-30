a, b = (1, 2)

def foo():
    return (3, 4)

fooA, fooB = foo()


# more python code to be used for more tests...
a = 42
a, b = [21, 42]
c = d = 42
e = 42, 42
f, g = '42' # yes, this also unpacks strings!

class Foo:
    foo = 42

a, newVar, Foo.foo, Foo.bar, *rest = 1, 2, 3, 4, 5, 6
