class Bar
{
    int x;

    int Add(int a, int b)
    {
        return a + b;
    }

    void memberCall()
    {
        this.Add(1, 2);
    }

    void simpleCall()
    {
        Add(3, 4);
    }

    int Offset(int a, int b = 10)
    {
        return a + b;
    }

    void callWithOmittedArgument()
    {
        Offset(1);
    }

    void nestedCall()
    {
        var sum = Add(1, 2);
        Add(Add(sum, 3), 4);
    }

    int localFunctions(int a)
    {
        int Twice(int v) => v * 2;
        return Square(a) + Twice(a);

        int Square(int v)
        {
            return v * v;
        }
    }
}
