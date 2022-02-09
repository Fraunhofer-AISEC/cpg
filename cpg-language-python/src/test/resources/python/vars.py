class Foo:
    classFieldNoInitializer: int
    classFieldWithInit = 123
    classFieldNoInitializer = classFieldWithInit

    def bar(self):
        self.classFieldDeclaredInFunction = 456
        self.classFieldNoInitializer = 789
        self.classFieldWithInit = 12
        classFieldNoInitializer = "shadowed"
        classFieldWithInit = "shadowed"
        classFieldDeclaredInFunction = "shadowed"

foo = Foo()
foo.classFieldNoInitializer = 345
foo.classFieldWithInit = 678
