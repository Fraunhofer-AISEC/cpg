class Foo
{
    void prefixOperators()
    {
        int a = 5;
        int b = -a;
        bool c = true;
        bool d = !c;
        int e = ~a;
        ++a;
        --a;
    }

    void postfixOperators()
    {
        int a = 5;
        a++;
        a--;
    }
}