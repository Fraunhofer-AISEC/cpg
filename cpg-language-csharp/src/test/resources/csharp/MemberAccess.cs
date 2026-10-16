class Foo
{
    int x;

    void simpleMemberAccess(Bar obj)
    {
        int a = obj.b;
    }

    void thisMemberAccess()
    {
        int a = this.x;
    }

    void thisFieldAccess()
    {
        this.x = 42;
    }
}

class Bar {
    string b;
}