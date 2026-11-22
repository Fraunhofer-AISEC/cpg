using System;
using System.IO;

namespace Test
{
    class UsingStatements
    {
        // using statement declaring its resource
        void UsingDeclaration()
        {
            using (TextWriter w = File.CreateText("log.txt"))
            {
                w.WriteLine("This is line one");
                w.WriteLine("This is line two");
            }
        }

        // using statement over an already existing value, without a declaration
        void UsingExpression(TextWriter w)
        {
            using (w)
            {
                w.WriteLine("This is line one");
            }
        }

        // using statement with a single statement instead of a block
        void UsingWithoutBlock()
        {
            using (TextWriter w = File.CreateText("log.txt")) w.WriteLine("This is line one");
        }

        // multiple resources in one using statement, disposed in reverse order
        void MultipleResources()
        {
            using (TextWriter a = File.CreateText("a.txt"), b = File.CreateText("b.txt"))
            {
                a.WriteLine("This is line one");
                b.WriteLine("This is line two");
            }
        }

        // using declaration, scoped to the rest of the enclosing block
        void UsingDeclarationStatement()
        {
            var path = "log.txt";
            using var w = File.CreateText(path);
            w.WriteLine("This is line one");
        }

        // two using declarations in one block, disposed in reverse order
        void MultipleUsingDeclarations()
        {
            using var a = File.CreateText("a.txt");
            using var b = File.CreateText("b.txt");
            a.WriteLine("This is line one");
        }

        // a using declaration nested in a block that is not a method body
        void UsingDeclarationInTry()
        {
            try
            {
                using var w = File.CreateText("log.txt");
                w.WriteLine("This is line one");
            }
            catch (IOException e)
            {
                Log(e);
            }
        }

        // nested using statements
        void NestedUsings()
        {
            using (TextWriter outer = File.CreateText("outer.txt"))
            {
                using (TextWriter inner = File.CreateText("inner.txt"))
                {
                    inner.WriteLine("This is line one");
                }

                outer.WriteLine("This is line two");
            }
        }

        void Log(object e) { }
    }
}