class Foo
{
    int x;

    Foo(int x)
    {
        this.x = x;
    }
}

class Bar
{
    void createFoo()
    {
        Foo f = new Foo(1);
    }
}

class Baz
{
    // implicit constructor call
    Foo foo = new(1);
}

class FooBar
{
    Foo f = new Foo(1) { x = 2 };
}