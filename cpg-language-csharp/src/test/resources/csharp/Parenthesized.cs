namespace Test
{
    class Parenthesized
    {
        int Grouping(int a, int b, int c)
        {
            return (a + b) * c;
        }

        int Redundant(int a)
        {
            return ((a));
        }
    }
}