using System.Runtime.InteropServices;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.CSharp.Syntax;

namespace NativeParser;

public static class Library
{
    private static readonly Dictionary<int, object> Handles = new();
    private static int _nextId = 1;

    private static IntPtr Save(object obj)
    {
        var id = _nextId++;
        Handles[id] = obj;
        return (IntPtr)id;
    }

    private static T Restore<T>(IntPtr handle)
    {
        return (T)Handles[(int)handle];
    }

    [UnmanagedCallersOnly(EntryPoint = "parseCsharp")]
    public static IntPtr ParseCSharp(IntPtr sourcePtr)
    {
        var source = Marshal.PtrToStringUTF8(sourcePtr);
        var tree = CSharpSyntaxTree.ParseText(source);
        var root = tree.GetRoot();
        return Save(root);
    }

    [UnmanagedCallersOnly(EntryPoint = "getKind")]
    public static IntPtr GetKind(IntPtr handle)
    {
        var node = Restore<CSharpSyntaxNode>(handle);
        return Marshal.StringToCoTaskMemUTF8(node.Kind().ToString());
    }

    [UnmanagedCallersOnly(EntryPoint = "getCode")]
    public static IntPtr GetCode(IntPtr handle)
    {
        var node = Restore<CSharpSyntaxNode>(handle);
        return Marshal.StringToCoTaskMemUTF8(node.ToFullString().Trim());
    }

    [UnmanagedCallersOnly(EntryPoint = "getNumChildren")]
    public static int GetNumChildren(IntPtr handle)
    {
        var node = Restore<CSharpSyntaxNode>(handle);
        return node.ChildNodes().Count();
    }

    [UnmanagedCallersOnly(EntryPoint = "getChild")]
    public static IntPtr GetChild(IntPtr handle, int index)
    {
        var node = Restore<CSharpSyntaxNode>(handle);
        return Save(node.ChildNodes().ElementAt(index));
    }

    [UnmanagedCallersOnly(EntryPoint = "getIdentifier")]
    public static IntPtr GetIdentifier(IntPtr handle)
    {
        var node = Restore<CSharpSyntaxNode>(handle);
        var text = node switch
        {
            ClassDeclarationSyntax cls => cls.Identifier.Text,
            _ => "UNKNOWN"
        };
        return Marshal.StringToCoTaskMemUTF8(text);
    }

    [UnmanagedCallersOnly(EntryPoint = "getName")]
    public static IntPtr GetName(IntPtr handle)
    {
        var node = Restore<CSharpSyntaxNode>(handle);
        var text = node switch
        {
            BaseNamespaceDeclarationSyntax ns => ns.Name.ToString(),
            _ => "UNKNOWN"
        };
        return Marshal.StringToCoTaskMemUTF8(text);
    }

    [UnmanagedCallersOnly(EntryPoint = "freeString")]
    public static void FreeString(IntPtr ptr)
    {
        Marshal.FreeCoTaskMem(ptr);
    }
}