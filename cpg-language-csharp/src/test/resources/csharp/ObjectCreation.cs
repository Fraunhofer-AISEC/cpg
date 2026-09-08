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

class Point
{
    int X;
    int Y;
}

class Rectangle
{
    Point P1;
    Point P2;

    void create()
    {
        Rectangle r = new Rectangle
        {
            P1 = new Point { X = 0, Y = 1 },
            P2 = new Point { X = 2, Y = 3 }
        };
    }
}

class Rectangle2
{
    Point P1 = new Point();
    Point P2 = new Point();

    void create()
    {
        Rectangle2 r2 = new Rectangle2
        {
            P1 = { X = 0, Y = 1 },
            P2 = { X = 2, Y = 3 }
        };
    }
}