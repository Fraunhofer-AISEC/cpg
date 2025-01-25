import os

a = "Hello"

class MyClass:
    def foo(self):
        self.a = 1
        print(a)

    def bar(self):
        self.os = 1
        print(os.name)


m = MyClass()
m.foo()
m.bar()
