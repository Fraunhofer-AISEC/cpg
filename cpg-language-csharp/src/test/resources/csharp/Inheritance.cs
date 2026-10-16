public interface IBase
{
}

public abstract class Foo : IBase
{
    int x;

    public int DoSomething()
    {
        return x;
    }
}

public class GenericClass<T>
{
}

class Bar : Foo
{
    int y;

    int AccessField()
    {
        return this.x;
    }

    int CallInheritedMethod()
    {
        return this.DoSomething();
    }
}