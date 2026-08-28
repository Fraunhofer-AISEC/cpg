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
}