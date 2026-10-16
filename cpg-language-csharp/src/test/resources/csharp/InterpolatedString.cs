class Foo
{
    string greet(string name)
    {
        return $"Hello {name}!";
    }

    string constant()
    {
        return $"Hello";
    }

    string formatted(double value)
    {
        return $"Value: {value:F2}";
    }
}