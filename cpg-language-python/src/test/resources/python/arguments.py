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

def kw_args_and_default(a, b=False, **kwargs):
    pass

def defaults(b=1, c=2, *d, e):
    pass

class MyClass:
    def my_method(self=5, d=3, e=1):
        pass

    def method_with_some_defaults(self, a, b=1, c=2):
        pass

    def call(self, a):
        kw_args_and_default(a)

    def call2(self, a):
        kw_args_and_default(a, True)

    def call3(self, a):
        kw_args_and_default(a, b=True, foo=1, bar=2, baz=3)

    def call4(self, a, b):
        kw_args_and_default(a, True, foo=1, bar=2, baz=3)

    def call5(self, a):
        kw_args_and_default(b=True, a=3)

