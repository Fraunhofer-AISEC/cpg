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

    private static void PrintASTDump(SyntaxNode node, int indent = 2)
          {
              Console.Error.WriteLine(new string(' ', indent) + node.GetType().Name + ": " + node.ToString().Split('\n')[0].Trim());
              foreach (var child in node.ChildNodes())
              PrintASTDump(child, indent + 1);
          }

    [UnmanagedCallersOnly(EntryPoint = "CSharpRoslynSyntaxTreeParseText")]
    public static IntPtr CSharpRoslynSyntaxTreeParseText(IntPtr sourcePtr)
    {
        // Kotlin sends the source code as string. JNA converts it into a C char*-pointer.
        // Marshal gets the pointer and converts it to an .NET string
        var source = Marshal.PtrToStringUTF8(sourcePtr);
        // Roslyn parses the source code -> AST
        var root = (CSharpSyntaxNode)CSharpSyntaxTree.ParseText(source).GetRoot();
//        PrintASTDump(root);
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

    // Cast to BaseNamespaceDeclarationSyntax to support both block-scoped (NamespaceDeclarationSyntax)
    // and file-scoped (FileScopedNamespaceDeclarationSyntax) namespaces.
    [UnmanagedCallersOnly(EntryPoint = "GetNamespaceDeclarationName")]
    public static IntPtr GetNamespaceDeclarationName(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((BaseNamespaceDeclarationSyntax)Nodes[handlePtr]).Name.ToString()
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetNamespaceDeclarationMembersCount")]
    public static int GetNamespaceDeclarationMembersCount(IntPtr handlePtr)
    {
        return ((BaseNamespaceDeclarationSyntax)Nodes[handlePtr]).Members.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetNamespaceDeclarationMember")]
    public static IntPtr GetNamespaceDeclarationMember(IntPtr handlePtr, int index)
    {
        return Register(((BaseNamespaceDeclarationSyntax)Nodes[handlePtr]).Members[index]);
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

    [UnmanagedCallersOnly(EntryPoint = "GetCode")]
    public static IntPtr GetCode(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(Nodes[handlePtr].ToString());
    }

    [UnmanagedCallersOnly(EntryPoint = "GetNodeStartLine")]
    public static int GetNodeStartLine(IntPtr handlePtr)
    {
        return Nodes[handlePtr].GetLocation().GetLineSpan().StartLinePosition.Line + 1;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetNodeStartColumn")]
    public static int GetNodeStartColumn(IntPtr handlePtr)
    {
        return Nodes[handlePtr].GetLocation().GetLineSpan().StartLinePosition.Character + 1;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetNodeEndLine")]
    public static int GetNodeEndLine(IntPtr handlePtr)
    {
        return Nodes[handlePtr].GetLocation().GetLineSpan().EndLinePosition.Line + 1;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetNodeEndColumn")]
    public static int GetNodeEndColumn(IntPtr handlePtr)
    {
        return Nodes[handlePtr].GetLocation().GetLineSpan().EndLinePosition.Character + 1;
    }
    [UnmanagedCallersOnly(EntryPoint = "GetFieldVariableCount")]
    public static int GetFieldVariableCount(IntPtr handlePtr)
    {
        return ((FieldDeclarationSyntax)Nodes[handlePtr]).Declaration.Variables.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetFieldVariable")]
    public static IntPtr GetFieldVariable(IntPtr handlePtr, int index)
    {
        return Register(((FieldDeclarationSyntax)Nodes[handlePtr]).Declaration.Variables[index]);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetVariableDeclaratorIdentifier")]
    public static IntPtr GetVariableDeclaratorIdentifier(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((VariableDeclaratorSyntax)Nodes[handlePtr]).Identifier.ToString()
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetMethodDeclarationParameterCount")]
    public static int GetMethodDeclarationParameterCount(IntPtr handlePtr)
    {
        return ((MethodDeclarationSyntax)Nodes[handlePtr]).ParameterList.Parameters.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetMethodDeclarationParameter")]
    public static IntPtr GetMethodDeclarationParameter(IntPtr handlePtr, int index)
    {
        return Register(((MethodDeclarationSyntax)Nodes[handlePtr]).ParameterList.Parameters[index]);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetParameterIdentifier")]
    public static IntPtr GetParameterIdentifier(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((ParameterSyntax)Nodes[handlePtr]).Identifier.ToString()
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetParameterType")]
    public static IntPtr GetParameterType(IntPtr handlePtr)
    {
        return Register(((ParameterSyntax)Nodes[handlePtr]).Type);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetConstructorDeclarationIdentifier")]
    public static IntPtr GetConstructorDeclarationIdentifier(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((ConstructorDeclarationSyntax)Nodes[handlePtr]).Identifier.ToString()
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetConstructorDeclarationParameterCount")]
    public static int GetConstructorDeclarationParameterCount(IntPtr handlePtr)
    {
        return ((ConstructorDeclarationSyntax)Nodes[handlePtr]).ParameterList.Parameters.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetConstructorDeclarationParameter")]
    public static IntPtr GetConstructorDeclarationParameter(IntPtr handlePtr, int index)
    {
        return Register(((ConstructorDeclarationSyntax)Nodes[handlePtr]).ParameterList.Parameters[index]);
    }

    // We need to call ToString() on the TypeSyntax node to get the actual type name
    // (e.g. "int", "string"). Otherwise, it would return the syntax node category
    // (e.g. "PredefinedType", "IdentifierName").
    [UnmanagedCallersOnly(EntryPoint = "GetTypeName")]
    public static IntPtr GetTypeName(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((TypeSyntax)Nodes[handlePtr]).ToString()
        );
    }

    // `BaseMethodDeclarationSyntax` to get the body for Methods and Constructors.
    [UnmanagedCallersOnly(EntryPoint = "GetBaseMethodDeclarationBody")]
    public static IntPtr GetBaseMethodDeclarationBody(IntPtr handlePtr)
    {
        return Register(((BaseMethodDeclarationSyntax)Nodes[handlePtr]).Body);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetBlockStatementCount")]
    public static int GetBlockStatementCount(IntPtr handlePtr)
    {
        return ((BlockSyntax)Nodes[handlePtr]).Statements.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetBlockStatement")]
    public static IntPtr GetBlockStatement(IntPtr handlePtr, int index)
    {
        return Register(((BlockSyntax)Nodes[handlePtr]).Statements[index]);
    }
}