enum Enums
{
    A,
    B = 5,
    C
}

class Foo
{
    Enums Bar()
    {
        return Enums.B;
    }
}

