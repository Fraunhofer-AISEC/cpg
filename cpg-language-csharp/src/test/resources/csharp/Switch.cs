class Foo
{
    int SwitchStmt(int x)
    {
        int counter = 0;
        switch (x)
        {
            case 1:
                counter = 10;
                break;
            case 2:
            case 3:
                counter = 20;
                break;
            default:
                counter = -1;
                break;
        }
        return counter;
    }

    void CaseWithDefault(int i)
    {
        switch (i)
        {
            case 0:
                CaseZero();
                break;
            case 1:
                CaseOne();
                break;
            case 2:
            default:
                CaseTwo();
                break;
        }
    }

    string StringSwitch(string command)
    {
        switch (command)
        {
            case "run":
                return "running";
            case "stop":
                return "stopped";
            default:
                return "unknown";
        }
    }

    string PatternMatchSwitch(string description)
    {
    // https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/statements#1383-the-switch-statement
        switch (description)
        {
            case "first case":
                return "first";
            case var a when a.Length > 10:
                return a;
            default:
                return "unknown";
        }
    }
}