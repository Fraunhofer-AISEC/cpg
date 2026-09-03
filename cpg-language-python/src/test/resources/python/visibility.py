class MyClass:
    # class attribute (public)
    public_attr = 1
    # single leading underscore -> conventionally non-public (PROTECTED)
    _protected_attr = 2
    # double leading underscore, no trailing dunder -> name-mangled (PRIVATE)
    __private_attr = 3
    # dunder -> public
    __magic__ = 4

    def __init__(self):
        # instance attributes with the same conventions
        self.public_field = 1
        self._protected_field = 2
        self.__private_field = 3

    def public_method(self):
        pass

    def _protected_method(self):
        pass

    def __private_method(self):
        pass

    def __str__(self):
        return "MyClass"


def free_function():
    pass
