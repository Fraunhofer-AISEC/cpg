namespace HelloWorld;

interface IShape
{
    int Area();
}

struct Point : IShape
{
    int x;
    int y;

    Point(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    int Area() => x * y;
}

struct Pair<T>
{
    T first;
    T second;
}

class Outer
{
    struct Nested
    {
        int value;
    }
}
