class SomeClass:
    def method(self, a):
        self.x = a
        b = a
        return b

class SomeClass2:
    @staticmethod
    def static_method(a):
        x = a
        b = x
        return b

def foo(fooA, b):
    fooA = bar(fooA)
    return fooA