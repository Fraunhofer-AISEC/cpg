class Other:
    j: int

class Foo:
    i: int

    def from_other(self, other: Other):
        self.i = other.j
