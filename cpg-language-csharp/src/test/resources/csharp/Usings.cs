// https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/namespaces#146-using-directives

// Global using directives must precede all non-global using directives.

// global using namespace directive
global using System.Text;
// global using static directive
global using static System.Console;
// global using alias directive
global using Env = System.Environment;

// using namespace directive
using System;
// using static directive
using static System.Math;
// using alias directive (alias for a namespace)
using Col = System.Collections.Generic;
// using alias directive (alias for a type)
using Str = System.String;
// using alias directive (alias for a closed constructed type)
using IntList = System.Collections.Generic.List<int>;

// TODO: extern alias directives and the `::` qualified-alias-member are not yet supported by the
//  frontend. `extern alias` is an ExternAliasDirectiveSyntax rather than a UsingDirectiveSyntax,
// extern alias LibA;
// using RootA = LibA::N;

namespace HelloWorld
{
    // using directive within a namespace body
    using System.IO;

    class Foo { }
}