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
        Console.Error.WriteLine(new string(' ', indent) + node.GetType().Name + " (Kind: " + ((CSharpSyntaxNode)node).Kind() + "): " + node.ToString().Split('\n')[0].Trim());
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

    [UnmanagedCallersOnly(EntryPoint = "GetType")]
    public static IntPtr GetType(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(Nodes[handlePtr].GetType().Name);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetCompilationUnitUsingsCount")]
    public static int GetCompilationUnitUsingsCount(IntPtr handlePtr)
    {
        return ((CompilationUnitSyntax)Nodes[handlePtr]).Usings.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetCompilationUnitUsing")]
    public static IntPtr GetCompilationUnitUsing(IntPtr handlePtr, int index)
    {
        return Register(((CompilationUnitSyntax)Nodes[handlePtr]).Usings[index]);
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

    [UnmanagedCallersOnly(EntryPoint = "GetNamespaceDeclarationUsingsCount")]
    public static int GetNamespaceDeclarationUsingsCount(IntPtr handlePtr)
    {
        return ((BaseNamespaceDeclarationSyntax)Nodes[handlePtr]).Usings.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetNamespaceDeclarationUsing")]
    public static IntPtr GetNamespaceDeclarationUsing(IntPtr handlePtr, int index)
    {
        return Register(((BaseNamespaceDeclarationSyntax)Nodes[handlePtr]).Usings[index]);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetUsingDirectiveName")]
    public static IntPtr GetUsingDirectiveName(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((UsingDirectiveSyntax)Nodes[handlePtr]).Name.ToString()
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetUsingDirectiveAlias")]
    public static IntPtr GetUsingDirectiveAlias(IntPtr handlePtr)
    {
        var alias = ((UsingDirectiveSyntax)Nodes[handlePtr]).Alias;
        return alias != null ? Register(alias) : IntPtr.Zero;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetUsingDirectiveStaticKeyword")]
    public static IntPtr GetUsingDirectiveStaticKeyword(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((UsingDirectiveSyntax)Nodes[handlePtr]).StaticKeyword.Text
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetUsingDirectiveGlobalKeyword")]
    public static IntPtr GetUsingDirectiveGlobalKeyword(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((UsingDirectiveSyntax)Nodes[handlePtr]).GlobalKeyword.Text
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetNameEqualsSyntaxName")]
    public static IntPtr GetNameEqualsSyntaxName(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((NameEqualsSyntax)Nodes[handlePtr]).Name.Identifier.Text
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetEnumDeclarationIdentifier")]
    public static IntPtr GetEnumDeclarationIdentifier(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((EnumDeclarationSyntax)Nodes[handlePtr]).Identifier.ToString()
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetEnumDeclarationMembersCount")]
    public static int GetEnumDeclarationMembersCount(IntPtr handlePtr)
    {
        return ((EnumDeclarationSyntax)Nodes[handlePtr]).Members.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetEnumDeclarationMember")]
    public static IntPtr GetEnumDeclarationMember(IntPtr handlePtr, int index)
    {
        return Register(((EnumDeclarationSyntax)Nodes[handlePtr]).Members[index]);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetEnumMemberDeclarationIdentifier")]
    public static IntPtr GetEnumMemberDeclarationIdentifier(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((EnumMemberDeclarationSyntax)Nodes[handlePtr]).Identifier.ToString()
        );
    }

    // Identifier for Class, Interface, Struct, Record, Enum.
    [UnmanagedCallersOnly(EntryPoint = "GetBaseTypeDeclarationIdentifier")]
    public static IntPtr GetBaseTypeDeclarationIdentifier(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((BaseTypeDeclarationSyntax)Nodes[handlePtr]).Identifier.ToString()
        );
    }

    // Members for Class, Interface, Struct, Record.
    [UnmanagedCallersOnly(EntryPoint = "GetTypeDeclarationMembersCount")]
    public static int GetTypeDeclarationMembersCount(IntPtr handlePtr)
    {
        return ((TypeDeclarationSyntax)Nodes[handlePtr]).Members.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetTypeDeclarationMember")]
    public static IntPtr GetTypeDeclarationMember(IntPtr handlePtr, int index)
    {
        return Register(((TypeDeclarationSyntax)Nodes[handlePtr]).Members[index]);
    }

    // BaseList can be null if the class has no base types (e.g. `class Foo { }`).
    // We use TypeDeclarationSyntax to support both ClassDeclarationSyntax and InterfaceDeclarationSyntax.
    [UnmanagedCallersOnly(EntryPoint = "GetTypeDeclarationBaseList")]
    public static IntPtr GetTypeDeclarationBaseList(IntPtr handlePtr)
    {
        var baseList = ((TypeDeclarationSyntax)Nodes[handlePtr]).BaseList;
        return baseList != null ? Register(baseList) : IntPtr.Zero;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetBaseListTypeCount")]
    public static int GetBaseListTypeCount(IntPtr handlePtr)
    {
        return ((BaseListSyntax)Nodes[handlePtr]).Types.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetBaseListType")]
    public static IntPtr GetBaseListType(IntPtr handlePtr, int index)
    {
        return Register(((BaseListSyntax)Nodes[handlePtr]).Types[index].Type);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetMemberDeclarationModifierCount")]
    public static int GetMemberDeclarationModifierCount(IntPtr handlePtr)
    {
        return ((MemberDeclarationSyntax)Nodes[handlePtr]).Modifiers.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetMemberDeclarationModifier")]
    public static IntPtr GetMemberDeclarationModifier(IntPtr handlePtr, int index)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((MemberDeclarationSyntax)Nodes[handlePtr]).Modifiers[index].Text
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetTypeParameterList")]
    public static IntPtr GetTypeParameterList(IntPtr handlePtr)
    {
        var typeParamList = ((TypeDeclarationSyntax)Nodes[handlePtr]).TypeParameterList;
        return typeParamList != null ? Register(typeParamList) : IntPtr.Zero;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetTypeParameterListCount")]
    public static int GetTypeParameterListCount(IntPtr handlePtr)
    {
        return ((TypeParameterListSyntax)Nodes[handlePtr]).Parameters.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetTypeParameterListParameter")]
    public static IntPtr GetTypeParameterListParameter(IntPtr handlePtr, int index)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((TypeParameterListSyntax)Nodes[handlePtr]).Parameters[index].Identifier.Text
        );
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
    [UnmanagedCallersOnly(EntryPoint = "GetFieldDeclaration")]
    public static IntPtr GetFieldDeclaration(IntPtr handlePtr)
    {
        return Register(((FieldDeclarationSyntax)Nodes[handlePtr]).Declaration);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetVariableDeclaratorIdentifier")]
    public static IntPtr GetVariableDeclaratorIdentifier(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(
            ((VariableDeclaratorSyntax)Nodes[handlePtr]).Identifier.ToString()
        );
    }

    [UnmanagedCallersOnly(EntryPoint = "GetBaseMethodDeclarationParameterList")]
    public static IntPtr GetBaseMethodDeclarationParameterList(IntPtr handlePtr)
    {
        return Register(((BaseMethodDeclarationSyntax)Nodes[handlePtr]).ParameterList);
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

    [UnmanagedCallersOnly(EntryPoint = "GetParameterListCount")]
    public static int GetParameterListCount(IntPtr handlePtr)
    {
        return ((ParameterListSyntax)Nodes[handlePtr]).Parameters.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetParameterListParameter")]
    public static IntPtr GetParameterListParameter(IntPtr handlePtr, int index)
    {
        return Register(((ParameterListSyntax)Nodes[handlePtr]).Parameters[index]);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetArgumentListCount")]
    public static int GetArgumentListCount(IntPtr handlePtr)
    {
        return ((ArgumentListSyntax)Nodes[handlePtr]).Arguments.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetArgumentListArgument")]
    public static IntPtr GetArgumentListArgument(IntPtr handlePtr, int index)
    {
        return Register(((ArgumentListSyntax)Nodes[handlePtr]).Arguments[index]);
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

    [UnmanagedCallersOnly(EntryPoint = "GetArrayTypeElementType")]
    public static IntPtr GetArrayTypeElementType(IntPtr handlePtr)
    {
        return Register(((ArrayTypeSyntax)Nodes[handlePtr]).ElementType);
    }

    // We use `BaseMethodDeclarationSyntax` to get the body for Methods and Constructors.
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

    [UnmanagedCallersOnly(EntryPoint = "GetReturnStatementExpression")]
    public static IntPtr GetReturnStatementExpression(IntPtr handlePtr)
    {
        var expression = ((ReturnStatementSyntax)Nodes[handlePtr]).Expression;
        if(expression != null)
        {
            return Register(expression);
        }
        else
        {
            return IntPtr.Zero;
        };
    }

    [UnmanagedCallersOnly(EntryPoint = "GetLiteralExpressionValue")]
    public static IntPtr GetLiteralExpressionValue(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(((LiteralExpressionSyntax)Nodes[handlePtr]).Token.Value.ToString());
    }

    [UnmanagedCallersOnly(EntryPoint = "GetIfStatementSyntaxCondition")]
    public static IntPtr GetIfStatementSyntaxCondition(IntPtr handlePtr)
    {
        return Register(((IfStatementSyntax)Nodes[handlePtr]).Condition);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetIfStatementSyntaxStatement")]
    public static IntPtr GetIfStatementSyntaxStatement(IntPtr handlePtr)
    {
        return Register(((IfStatementSyntax)Nodes[handlePtr]).Statement);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetElseClauseSyntax")]
    public static IntPtr GetElseClauseSyntax(IntPtr handlePtr)
    {
        var elseClause = ((IfStatementSyntax)Nodes[handlePtr]).Else;
        if (elseClause != null)
        {
            return Register(elseClause);
        }
        else
        {
            return IntPtr.Zero;
        }
    }

    [UnmanagedCallersOnly(EntryPoint = "GetElseClauseSyntaxStatement")]
    public static IntPtr GetElseClauseSyntaxStatement(IntPtr handlePtr)
    {
        return Register(((ElseClauseSyntax)Nodes[handlePtr]).Statement);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetBinaryExpressionLeft")]
    public static IntPtr GetBinaryExpressionLeft(IntPtr handlePtr)
    {
        return Register(((BinaryExpressionSyntax)Nodes[handlePtr]).Left);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetBinaryExpressionRight")]
    public static IntPtr GetBinaryExpressionRight(IntPtr handlePtr)
    {
        return Register(((BinaryExpressionSyntax)Nodes[handlePtr]).Right);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetBinaryExpressionOperator")]
    public static IntPtr GetBinaryExpressionOperator(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(((BinaryExpressionSyntax)Nodes[handlePtr]).OperatorToken.Text);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetIdentifierNameSyntaxIdentifier")]
    public static IntPtr GetIdentifierNameSyntaxIdentifier(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(((IdentifierNameSyntax)Nodes[handlePtr]).Identifier.ToString());
    }

    [UnmanagedCallersOnly(EntryPoint = "GetVariableDeclarationSyntax")]
    public static IntPtr GetVariableDeclarationSyntax(IntPtr handlePtr)
    {
        return Register(((LocalDeclarationStatementSyntax)Nodes[handlePtr]).Declaration);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetVariableDeclarationType")]
    public static IntPtr GetVariableDeclarationType(IntPtr handlePtr)
    {
        return Register(((VariableDeclarationSyntax)Nodes[handlePtr]).Type);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetLocalVariableCount")]
    public static int GetLocalVariableCount(IntPtr handlePtr)
    {
        return ((VariableDeclarationSyntax)Nodes[handlePtr]).Variables.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetLocalVariable")]
    public static IntPtr GetLocalVariable(IntPtr handlePtr, int index)
    {
        return Register(((VariableDeclarationSyntax)Nodes[handlePtr]).Variables[index]);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetVariableDeclaratorInitializer")]
    public static IntPtr GetVariableDeclaratorInitializer(IntPtr handlePtr)
    {
        var initializer = ((VariableDeclaratorSyntax)Nodes[handlePtr]).Initializer;
        if (initializer != null)
        {
            return Register(initializer.Value);
        }
        else
        {
            return IntPtr.Zero;
        }
    }

    [UnmanagedCallersOnly(EntryPoint = "GetExpressionStatementExpression")]
    public static IntPtr GetExpressionStatementExpression(IntPtr handlePtr)
    {
        return Register(((ExpressionStatementSyntax)Nodes[handlePtr]).Expression);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetAssignmentExpressionLeft")]
    public static IntPtr GetAssignmentExpressionLeft(IntPtr handlePtr)
    {
        return Register(((AssignmentExpressionSyntax)Nodes[handlePtr]).Left);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetAssignmentExpressionRight")]
    public static IntPtr GetAssignmentExpressionRight(IntPtr handlePtr)
    {
        return Register(((AssignmentExpressionSyntax)Nodes[handlePtr]).Right);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetAssignmentExpressionOperator")]
    public static IntPtr GetAssignmentExpressionOperator(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(((AssignmentExpressionSyntax)Nodes[handlePtr]).OperatorToken.Text);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetWhileStatementCondition")]
    public static IntPtr GetWhileStatementCondition(IntPtr handlePtr)
    {
        return Register(((WhileStatementSyntax)Nodes[handlePtr]).Condition);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetWhileStatementStatement")]
    public static IntPtr GetWhileStatementStatement(IntPtr handlePtr)
    {
        return Register(((WhileStatementSyntax)Nodes[handlePtr]).Statement);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetDoStatementCondition")]
    public static IntPtr GetDoStatementCondition(IntPtr handlePtr)
    {
        return Register(((DoStatementSyntax)Nodes[handlePtr]).Condition);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetDoStatementStatement")]
    public static IntPtr GetDoStatementStatement(IntPtr handlePtr)
    {
        return Register(((DoStatementSyntax)Nodes[handlePtr]).Statement);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetForStatementDeclaration")]
    public static IntPtr GetForStatementDeclaration(IntPtr handlePtr)
    {
        var declaration = ((ForStatementSyntax)Nodes[handlePtr]).Declaration;
        return declaration != null ? Register(declaration) : IntPtr.Zero;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetForStatementInitializerExpressionCount")]
    public static int GetForStatementInitializerExpressionCount(IntPtr handlePtr)
    {
        return ((ForStatementSyntax)Nodes[handlePtr]).Initializers.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetForStatementInitializerExpression")]
    public static IntPtr GetForStatementInitializerExpression(IntPtr handlePtr, int index)
    {
        return Register(((ForStatementSyntax)Nodes[handlePtr]).Initializers[index]);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetForStatementCondition")]
    public static IntPtr GetForStatementCondition(IntPtr handlePtr)
    {
        var condition = ((ForStatementSyntax)Nodes[handlePtr]).Condition;
        return condition != null ? Register(condition) : IntPtr.Zero;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetForStatementIncrementorCount")]
    public static int GetForStatementIncrementorCount(IntPtr handlePtr)
    {
        return ((ForStatementSyntax)Nodes[handlePtr]).Incrementors.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetForStatementIncrementor")]
    public static IntPtr GetForStatementIncrementor(IntPtr handlePtr, int index)
    {
        return Register(((ForStatementSyntax)Nodes[handlePtr]).Incrementors[index]);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetForStatementStatement")]
    public static IntPtr GetForStatementStatement(IntPtr handlePtr)
    {
        return Register(((ForStatementSyntax)Nodes[handlePtr]).Statement);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetForEachStatementIdentifier")]
    public static IntPtr GetForEachStatementIdentifier(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(((ForEachStatementSyntax)Nodes[handlePtr]).Identifier.ToString());
    }

    [UnmanagedCallersOnly(EntryPoint = "GetForEachStatementType")]
    public static IntPtr GetForEachStatementType(IntPtr handlePtr)
    {
        return Register(((ForEachStatementSyntax)Nodes[handlePtr]).Type);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetForEachStatementExpression")]
    public static IntPtr GetForEachStatementExpression(IntPtr handlePtr)
    {
        return Register(((ForEachStatementSyntax)Nodes[handlePtr]).Expression);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetForEachStatementStatement")]
    public static IntPtr GetForEachStatementStatement(IntPtr handlePtr)
    {
        return Register(((ForEachStatementSyntax)Nodes[handlePtr]).Statement);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetInvocationExpressionExpression")]
    public static IntPtr GetInvocationExpressionExpression(IntPtr handlePtr)
    {
        return Register(((InvocationExpressionSyntax)Nodes[handlePtr]).Expression);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetInvocationExpressionArgumentList")]
    public static IntPtr GetInvocationExpressionArgumentList(IntPtr handlePtr)
    {
        return Register(((InvocationExpressionSyntax)Nodes[handlePtr]).ArgumentList);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetArgumentExpression")]
    public static IntPtr GetArgumentExpression(IntPtr handlePtr)
    {
        return Register(((ArgumentSyntax)Nodes[handlePtr]).Expression);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetMemberAccessExpressionExpression")]
    public static IntPtr GetMemberAccessExpressionExpression(IntPtr handlePtr)
    {
        return Register(((MemberAccessExpressionSyntax)Nodes[handlePtr]).Expression);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetMemberAccessExpressionName")]
    public static IntPtr GetMemberAccessExpressionName(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(((MemberAccessExpressionSyntax)Nodes[handlePtr]).Name.Identifier.ToString());
    }

    [UnmanagedCallersOnly(EntryPoint = "GetMemberAccessExpressionOperatorToken")]
    public static IntPtr GetMemberAccessExpressionOperatorToken(IntPtr handlePtr)
    {
        return Marshal.StringToCoTaskMemUTF8(((MemberAccessExpressionSyntax)Nodes[handlePtr]).OperatorToken.Text);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetObjectCreationExpressionType")]
    public static IntPtr GetObjectCreationExpressionType(IntPtr handlePtr)
    {
        return Register(((ObjectCreationExpressionSyntax)Nodes[handlePtr]).Type);
    }

    [UnmanagedCallersOnly(EntryPoint = "GetBaseObjectCreationExpressionArgumentList")]
    public static IntPtr GetBaseObjectCreationExpressionArgumentList(IntPtr handlePtr)
    {
        var argumentList = ((BaseObjectCreationExpressionSyntax)Nodes[handlePtr]).ArgumentList;
        return argumentList != null ? Register(argumentList) : IntPtr.Zero;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetBaseObjectCreationExpressionInitializer")]
    public static IntPtr GetBaseObjectCreationExpressionInitializer(IntPtr handlePtr)
    {
        var initializer = ((BaseObjectCreationExpressionSyntax)Nodes[handlePtr]).Initializer;
        return initializer != null ? Register(initializer) : IntPtr.Zero;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetInitializerExpressionExpressionsCount")]
    public static int GetInitializerExpressionExpressionsCount(IntPtr handlePtr)
    {
        return ((InitializerExpressionSyntax)Nodes[handlePtr]).Expressions.Count;
    }

    [UnmanagedCallersOnly(EntryPoint = "GetInitializerExpressionExpression")]
    public static IntPtr GetInitializerExpressionExpression(IntPtr handlePtr, int index)
    {
        return Register(((InitializerExpressionSyntax)Nodes[handlePtr]).Expressions[index]);
    }
}