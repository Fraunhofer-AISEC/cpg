using System.Runtime.InteropServices;
using Microsoft.CodeAnalysis.CSharp;

namespace NativeParser;

public static class Library
{
    [UnmanagedCallersOnly(EntryPoint = "parseCsharp")]
    public static IntPtr ParseCSharp(IntPtr sourcePtr)
    {
        var source = Marshal.PtrToStringUTF8(sourcePtr);
        var tree = CSharpSyntaxTree.ParseText(source);
        var root = tree.GetRoot();
        return Marshal.StringToCoTaskMemUTF8(root.Kind().ToString());
    }
}