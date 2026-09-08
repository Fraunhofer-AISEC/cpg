namespace HelloWorld;

class Foo
{
    void Bar() { }

    void Baz(int a, string b) { }

    void Optional(int x = 5, string label = "none", int? limit = null) { }

    int returnSomething()
    {
        return 1;
    }

    void returnWithoutExpression()
    {
        return;
    }

    int expressionBodied() => 1;

    void voidExpressionBodied() => Bar();
}