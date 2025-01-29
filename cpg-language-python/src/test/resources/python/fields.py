import os

a = "Hello"

class MyClass:
    copyA = 1
    def foo(self):
        self.a = 1
        print(a)

    def bar(self):
        self.os = 1
        print(os.name)
        self.a = 1

    def baz(self):
        print(self.copyA)
        doesNotWork(copyA) # not defined

m = MyClass()
m.foo()
m.bar()
m.baz()
