using System;

class Lambdas
{
    int offset;

    void lambdas()
    {
        Func<int, int> inc = x => x + 1;
        Func<int, int, int> add = (int a, int b) =>
        {
            return a + b + offset;
        };
        Func<int, int> twice = delegate (int v) { return v * 2; };
        Action noParameters = delegate { };
    }
}
