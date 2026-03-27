namespace Loops;

class Foo
{
    void whileLoop()
    {
        int i = 0;
        while (i < 10)
        {
            i += 1;
        }
    }

    void doWhileLoop()
    {
        int i = 0;
        do
        {
            i += 1;
        } while (i < 10);
    }

    void forLoop()
    {
        for (int i = 0; i < 10; i += 1)
        {
            int x = i;
        }
    }

    void forLoopMultipleIncrementors()
    {
        for (int i = 0, j = 10; i < j; i += 1, j -= 1)
        {
            int x = i;
        }
    }

    void forEachLoop()
    {
        int[] numbers = null;
        foreach (int n in numbers)
        {
            int x = n;
        }
    }
}