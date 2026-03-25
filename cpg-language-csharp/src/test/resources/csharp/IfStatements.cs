class Bar
{
    int doIf(int a)
    {
        if(a < 10)
        {
            return 1;
        }
        return 0;
    }

    int doIfElse(int a)
    {
        if(a < 10)
        {
            return 1;
        }
        else
        {
            return 2;
        }
    }

    int doIfElseIf(int a)
    {
        if(a < 10)
        {
            return 1;
        }
        else if(a < 20)
        {
            return 2;
        }
        else
        {
            return 3;
        }
    }
}