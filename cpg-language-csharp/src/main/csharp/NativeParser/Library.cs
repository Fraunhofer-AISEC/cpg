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

    [UnmanagedCallersOnly(EntryPoint = "GetCompilationUnitMembersCount")]
    public static int GetCompilationUnitMembersCount(IntPtr handlePtr)
    {
        return ((CompilationUnitSyntax)Nodes[handlePtr]).Members.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetCompilationUnitMember")]
    public static IntPtr GetCompilationUnitMember(IntPtr handlePtr, int index)
    {
        return Register(((CompilationUnitSyntax)Nodes[handlePtr]).Members[index]);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetNamespaceDeclarationName")]
    public static IntPtr GetNamespaceDeclarationName(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((NamespaceDeclarationSyntax)Nodes[handlePtr]).Name.ToString()
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetNamespaceDeclarationMembersCount")]
    public static int GetNamespaceDeclarationMembersCount(IntPtr handlePtr)
    {
        return ((NamespaceDeclarationSyntax)Nodes[handlePtr]).Members.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetNamespaceDeclarationMember")]
    public static IntPtr GetNamespaceDeclarationMember(IntPtr handlePtr, int index)
    {
        return Register(((NamespaceDeclarationSyntax)Nodes[handlePtr]).Members[index]);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetClassDeclarationIdentifier")]
    public static IntPtr GetClassDeclarationIdentifier(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((ClassDeclarationSyntax)Nodes[handlePtr]).Identifier.ToString()
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetClassDeclarationMembersCount")]
    public static int GetClassDeclarationMembersCount(IntPtr handlePtr)
    {
        return ((ClassDeclarationSyntax)Nodes[handlePtr]).Members.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetClassDeclarationMember")]
    public static IntPtr GetClassDeclarationMember(IntPtr handlePtr, int index)
    {
        return Register(((ClassDeclarationSyntax)Nodes[handlePtr]).Members[index]);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetMethodDeclarationIdentifier")]
    public static IntPtr GetMethodDeclarationIdentifier(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((MethodDeclarationSyntax)Nodes[handlePtr]).Identifier.ToString()
        );
    }
}