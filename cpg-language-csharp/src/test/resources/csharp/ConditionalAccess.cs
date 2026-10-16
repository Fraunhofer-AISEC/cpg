class Foo
{
    string memberAccess(Bar bar)
    {
        return bar?.b;
    }

    string invocation(Bar bar)
    {
        return bar?.ToString();
    }

    int? elementAccess(int[] items)
    {
        return items?[0];
    }

    string chainedMemberAccess(Bar bar)
    {
        return bar?.nested.c;
    }
}

class Bar
{
    string b;
    Nested nested;
}

class Nested
{
    string c;
}