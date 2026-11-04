#[derive(Clone, Copy, PartialEq, Eq, Debug, Hash)]
#[repr(u8)]
pub enum Kind {
    Unit, Function, Param, Variable, Record, Field, Typedef, Enum, Enumerator, Block, If, While,
    DoWhile, For, Switch, Case, Return, Break, Continue, Goto, Label, ExprStmt, Call, Binary,
    Unary, Assign, Ref, Literal, Member, Subscript, Cast, Conditional, Sizeof, InitList, Other,
    DeclStmt, PointerRef, PointerDeref, Constructor, ArrayConstruction, Range, Problem, ShortCircuit, Default, Empty, TypeReference, ExprList,
}

impl Kind {
    /// The simple class name of the corresponding node in the Kotlin CPG.
    pub fn class_name(self) -> &'static str {
        use Kind::*;
        match self {
            Unit => "TranslationUnit", Function => "Function", Param => "Parameter",
            Variable => "Variable", Record => "Record", Field => "Field", Typedef => "Typedef",
            Enum => "Enumeration", Enumerator => "EnumConstant", Block => "Block", If => "IfElse",
            While => "While", DoWhile => "DoWhile", For => "For", Switch => "Switch", Case => "Case",
            Return => "Return", Break => "Break", Continue => "Continue", Goto => "Goto",
            Label => "Label", ExprStmt => "ExpressionStatement", Call => "Call",
            Binary => "BinaryOperator", Unary => "UnaryOperator", Assign => "Assign",
            Ref => "Reference", Literal => "Literal", Member => "MemberAccess",
            Subscript => "Subscription", Cast => "Cast", Conditional => "Conditional",
            Sizeof => "Sizeof", InitList => "InitializerList", Other => "Other",
            DeclStmt => "DeclarationStatement", PointerRef => "PointerReference",
            PointerDeref => "PointerDereference", Constructor => "Constructor",
            ArrayConstruction => "ArrayConstruction", Range => "Range",
            Problem => "ProblemExpression", ShortCircuit => "ShortCircuitOperator",
            Default => "Default", Empty => "Empty", TypeReference => "TypeReference",
            ExprList => "ExpressionList",
        }
    }
}
