def function():
    d = Foo(
        "b",
        "d",
        "f"
    )

    # Test DFGs for member accesses
    d.b = 10
    print(d.b)
    print(d.a)
    print(d)

class Foo:
    a = 1
    b = 2
    c = 3

    def __init__(self, a, b, c):
        self.a = a
        self.b = b
        self.c = c