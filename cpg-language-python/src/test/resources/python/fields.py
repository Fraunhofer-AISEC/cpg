import os

a = "Hello"

class MyClass:
    copyA = a
    def foo(self):
        self.a = 1
        print(a)

    def bar(self):
        self.os = 1
        print(os.name)

    def baz(self):
        print(self.copyA)
        doesNotWork(copyA) # not defined

m = MyClass()
m.foo()
m.bar()
m.baz()
