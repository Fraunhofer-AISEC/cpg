class Foo:
    def __init__(self, a):
        self.a = a


def bar():
    x = "baz"
    f = Foo(x)
    return f