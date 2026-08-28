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
}