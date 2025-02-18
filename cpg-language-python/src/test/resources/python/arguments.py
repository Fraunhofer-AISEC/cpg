def pos_only_and_args(a, b, /, c):
    pass


def test_varargs(*args):
    pass


def kwd_only_arg(*, arg):
    pass


def kw_defaults(b=1, *, c=2, d, e=3):
    pass


def kw_args(**kwargs):
    pass


def kwArgsAndDefault(a, b=False, **kwargs):
    pass


def defaults(b=1, c=2, *d, e):
    pass


class MyClass:
    def my_method(self=5, d=3, e=1):
        pass

    def method_with_some_defaults(self, a, b=1, c=2):
        pass

    def call(self, a):
        kwArgsAndDefault(a)

    def call2(self, a, b):
        kwArgsAndDefault(a, True)

    def call3(self, a):
        kwArgsAndDefault(a, foo=1, bar=2, baz=3)

    def call4(self, a, b):
        kwArgsAndDefault(a, True, foo=1, bar=2, baz=3)
