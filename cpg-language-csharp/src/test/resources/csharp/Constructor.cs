namespace HelloWorld;

class Foo
{
    int x;

    Foo() { }

    Foo(int x, string y)
    {
        this.x = x;
    }

    Foo(int x) => this.x = x;

    Foo(int x, string y, int z = 42) { }
}

class Base
{
    Base(int x) { }
}

class Derived : Base
{
    Derived(int x) : base(x) { }

    Derived() : this(1) => Init();

    void Init() { }
}
