class Foo:
    def bar(self):
        return Bar()


class Bar:
    def baz(self):
        if True:
            return "bar"
        else:
            return 2


my = Foo() # "my" is of type Foo
bar = my.bar() # "bar" is of type Bar
a = bar.baz() # "a" is of type str | int
