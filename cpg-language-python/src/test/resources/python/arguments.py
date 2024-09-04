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

def defaults(b=1, c=2, *d, e):
    pass

class MyClass:
    def my_method(self, d, e):
        pass
