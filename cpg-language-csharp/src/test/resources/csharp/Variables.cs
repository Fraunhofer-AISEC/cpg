class Foo
{
    void bar()
    {
        // explicitly typed
        int a = 1;
        string b = "2";
        // implicitly typed
        var c = 5;
        var d = "Hello";
    }

    void multipleDeclarations()
    {
        // multiple variables in one statement
        int a = 1, b = 2, c = 3;
    }

    void withoutInitializer()
    {
        // declaration without initializer
        int a;
        string b;
    }

    void withInitializer()
    {
        int a = 1;
        int b = a + 2;
    }

}