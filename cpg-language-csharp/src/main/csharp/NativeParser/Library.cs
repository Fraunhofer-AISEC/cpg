using System.Runtime.InteropServices;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.CSharp.Syntax;

namespace NativeParser;

public static class Library
{
    private static readonly Dictionary<IntPtr, CSharpSyntaxNode> Nodes = new();
    private static int _nextId = 1;

    private static IntPtr Register(CSharpSyntaxNode node)
    {
        var ptr = new IntPtr(_nextId++);
        Nodes[ptr] = node;
        return ptr;
    }

    [UnmanagedCallersOnly(EntryPoint = "CSharpRoslynSyntaxTreeParseText")]
    public static IntPtr CSharpRoslynSyntaxTreeParseText(IntPtr sourcePtr)
    {
        // Kotlin sends the source code as string. JNA converts it into a C char*-pointer.
        // Marshal gets the pointer and converts it to an .NET string
        var source = Marshal.PtrToStringUTF8(sourcePtr);
        // Roslyn parses the source code -> AST
        var root = (CSharpSyntaxNode)CSharpSyntaxTree.ParseText(source).GetRoot();
        return Register(root);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetKind")]
    public static IntPtr GetKind(IntPtr handlePtr)
    {
        var kind = Nodes[handlePtr].Kind().ToString();
        return Marshal.StringToCoTaskMemUTF8(kind);
    }
}