def pos_only_and_args(a, b, /, c):
    pass

def kwd_only_arg(*, arg):
    pass

def kw_defaults(b=1, *, c=2, d, e=3):
    pass

def kw_args(**kwargs):
    pass


class MyClass:
    def my_method(self, d, e):
        pass
