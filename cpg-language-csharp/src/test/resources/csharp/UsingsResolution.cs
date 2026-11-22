namespace MyLib
{
    class Foo { }

    class Bar
    {
        public static int Make() { return 1; }
    }
}

namespace App
{
    using MyLib;
    using AliasedFoo = MyLib.Foo;
    using static MyLib.Bar;

    class Program
    {
        // Fully qualified: baseline, does not depend on any using directive.
        MyLib.Foo qualified;

        // Unqualified: resolves only through `using MyLib;`.
        Foo viaNamespace;

        // Resolves only through the `using AliasedWidget = MyLib.Foo;` alias.
        AliasedFoo viaAlias;

        int CallStatic()
        {
            // Unqualified static call: resolves only through `using static MyLib.Bar;`.
            return Make();
        }
    }
}
