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
        Foo f = new Foo(42);
    }
}