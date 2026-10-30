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

    static void use(int v) { }

    void siblingBlocks()
    {
        // each block declares its own variable
        {
            int x = 1;
            use(x);
        }
        {
            int x = 2;
            use(x);
        }
    }

    void nestedBlock(bool c)
    {
        int y = 0;
        if (c)
        {
            int z = y;
            use(z);
        }
        use(y);
    }
}
