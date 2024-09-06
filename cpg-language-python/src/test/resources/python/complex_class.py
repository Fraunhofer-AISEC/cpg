class Foo:
    x = 1
    s: str

    def bar(self, y: int):
        z = self.x + y
        self.x = z
        return z
