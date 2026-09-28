class Foo
{
    int[] sized(int n)
    {
        return new int[n];
    }

    int[] initialized()
    {
        return new int[] { 1, 2, 3 };
    }

    int[] implicitlyTyped()
    {
        return new[] { 1, 2, 3 };
    }
}