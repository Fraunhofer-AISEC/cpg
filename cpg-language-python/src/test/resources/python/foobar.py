def foo(a):
    return a+1

def bar():
    return foo(42)


class Foobar:
    def bar(self):
        return self.foo(41, 1)

    @staticmethod
    def foo(a, b):
        return a+b
