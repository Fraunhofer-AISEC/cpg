uniffi::setup_scaffolding!();

use std::fs;
use ra_ap_syntax::{ast, SourceFile, SyntaxNode};
use ra_ap_syntax::{AstNode, Edition};
use ra_ap_syntax::ast::{ArrayExpr, AsmExpr, AwaitExpr, BecomeExpr, BinExpr, BlockExpr, BreakExpr, CallExpr, CastExpr, ClosureExpr, Const, ContinueExpr, Enum, Expr, ExprStmt, ExternBlock, ExternCrate, FieldExpr, Fn, ForExpr, FormatArgsExpr, IfExpr, Impl, IndexExpr, Item, LetExpr, LetStmt, Literal, LoopExpr, MacroCall, MacroDef, MacroExpr, MacroRules, MatchExpr, MethodCallExpr, Module, OffsetOfExpr, ParenExpr, PathExpr, PrefixExpr, RangeExpr, RecordExpr, RefExpr, ReturnExpr, Static, Stmt, Struct, Trait, TryExpr, TupleExpr, TypeAlias, UnderscoreExpr, Union, Use, WhileExpr, YeetExpr, YieldExpr};


#[derive(uniffi::Record)]
pub struct RSSourceFile {
    pub(crate) ast_node: RSNode,
    pub path: String,
    pub items: Vec<RSAst>,
}




#[uniffi::export]
fn parse_rust_code(source: &str) -> Option<RSSourceFile>  {
    // This depends on what the parser API exactly is; this is a conceptual example
    let text = fs::read_to_string(source);

     match text {
         Ok(source_code) => Some(handle_source_file(SourceFile::parse(source_code.as_str(), Edition::CURRENT).tree())),
         Err(e) => None
     }

}


fn handle_source_file(file: SourceFile) -> RSSourceFile {
    let mut children = vec![];
    for child in file.syntax().children() {
        let o_item = Item::cast(child);
        if let Some(item) = o_item {
            let rs_item = item.into();
            children.push(RSAst::RustItem(rs_item));
        }

    }
    RSSourceFile{ast_node: file.syntax().into(), path : "".to_string(), items : children }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSNode {
    text: String,
    start_offset: u32,
    end_offset:u32
}

impl From<&SyntaxNode> for RSNode {
    fn from(syntax: &SyntaxNode) -> Self {
        RSNode {text : syntax.text().to_string(), start_offset: syntax.text_range().start().into(), end_offset: syntax.text_range().end().into()}
    }
}


#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSItem {
    AsmExpr(RSAsmExpr),
    Const(RSConst),
    Enum(RSEnum),
    ExternBlock(RSExternBlock),
    ExternCrate(RSExternCrate),
    Fn(RSFn),
    Impl(RSImpl),
    MacroCall(RSMacroCall),
    MacroDef(RSMacroDef),
    MacroRules(RSMacroRules),
    Module(RSModule),
    Static(RSStatic),
    Struct(RSStruct),
    Trait(RSTrait),
    TypeAlias(RSTypeAlias),
    Union(RSUnion),
    Use(RSUse),
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSAst {
    RustItem(RSItem),
    RustExpr(RSExpr),
    RustStmt(RSStmt)
}

impl From<Item> for RSItem {
    fn from(item: Item) -> Self {
        let ast_node = item.syntax().into();
        match item {
            Item::AsmExpr(node) => RSItem::AsmExpr(RSAsmExpr {ast_node}),
            Item::Const(node) => RSItem::Const(RSConst {ast_node}),
            Item::Enum(node) => RSItem::Enum(RSEnum {ast_node}),
            Item::ExternBlock(node) => RSItem::ExternBlock(RSExternBlock {ast_node}),
            Item::ExternCrate(node) => RSItem::ExternCrate(RSExternCrate {ast_node}),
            Item::Fn(node) => RSItem::Fn(RSFn {ast_node}),
            Item::Impl(node) => RSItem::Impl(RSImpl {ast_node}),
            Item::MacroCall(node) => RSItem::MacroCall(RSMacroCall {ast_node}),
            Item::MacroDef(node) => RSItem::MacroDef(RSMacroDef {ast_node}),
            Item::MacroRules(node) => RSItem::Module(RSModule {ast_node}),
            Item::Module(node) => RSItem::Module(RSModule {ast_node}),
            Item::Static(node) => RSItem::Static(RSStatic {ast_node}),
            Item::Struct(node) => RSItem::Struct(RSStruct {ast_node}),
            Item::Trait(node) => RSItem::Trait(RSTrait {ast_node}),
            Item::TypeAlias(node) => RSItem::TypeAlias(RSTypeAlias {ast_node}),
            Item::Union(node) => RSItem::Union(RSUnion {ast_node}),
            Item::Use(node) => RSItem::Use(RSUse {ast_node}),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSConst {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSEnum {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSExternBlock {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSExternCrate {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSFn {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSImpl {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroCall {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroDef {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroRules {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSModule {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSStatic {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSStruct {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTrait {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTypeAlias {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSUnion {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSUse {pub(crate) ast_node: RSNode}




#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSExpr {
    ArrayExpr(RSArrayExpr),
    AsmExpr(RSAsmExpr),
    AwaitExpr(RSAwaitExpr),
    BecomeExpr(RSBecomeExpr),
    BinExpr(RSBinExpr),
    BlockExpr(RSBlockExpr),
    BreakExpr(RSBreakExpr),
    CallExpr(RSCallExpr),
    CastExpr(RSCastExpr),
    ClosureExpr(RSClosureExpr),
    ContinueExpr(RSContinueExpr),
    FieldExpr(RSFieldExpr),
    ForExpr(RSForExpr),
    FormatArgsExpr(RSFormatArgsExpr),
    IfExpr(RSIfExpr),
    IndexExpr(RSIndexExpr),
    LetExpr(RSLetExpr),
    Literal(RSLiteral),
    LoopExpr(RSLoopExpr),
    MacroExpr(RSMacroExpr),
    MatchExpr(RSMatchExpr),
    MethodCallExpr(RSMethodCallExpr),
    OffsetOfExpr(RSOffsetOfExpr),
    ParenExpr(RSParenExpr),
    PathExpr(RSPathExpr),
    PrefixExpr(RSPrefixExpr),
    RangeExpr(RSRangeExpr),
    RecordExpr(RSRecordExpr),
    RefExpr(RSRefExpr),
    ReturnExpr(RSReturnExpr),
    TryExpr(RSTryExpr),
    TupleExpr(RSTupleExpr),
    UnderscoreExpr(RSUnderscoreExpr),
    WhileExpr(RSWhileExpr),
    YeetExpr(RSYeetExpr),
    YieldExpr(RSYieldExpr),
}

impl From<Expr> for RSExpr {
    fn from(expr: Expr) -> Self {
        let ast_node = expr.syntax().into();
        match expr {
            Expr::ArrayExpr(ArrayExpr) => RSExpr::ArrayExpr(RSArrayExpr {ast_node}),
            Expr::AsmExpr(AsmExpr) => RSExpr::AsmExpr(RSAsmExpr {ast_node}),
            Expr::AwaitExpr(AwaitExpr) => RSExpr::AwaitExpr(RSAwaitExpr {ast_node}),
            Expr::BecomeExpr(BecomeExpr) => RSExpr::BecomeExpr(RSBecomeExpr {ast_node}),
            Expr::BinExpr(BinExpr) => RSExpr::BinExpr(RSBinExpr {ast_node}),
            Expr::BlockExpr(BlockExpr) => RSExpr::BlockExpr(RSBlockExpr {ast_node}),
            Expr::BreakExpr(BreakExpr) => RSExpr::BreakExpr(RSBreakExpr {ast_node}),
            Expr::CallExpr(CallExpr) => RSExpr::CallExpr(RSCallExpr {ast_node}),
            Expr::CastExpr(CastExpr) => RSExpr::CastExpr(RSCastExpr {ast_node}),
            Expr::ClosureExpr(ClosureExpr) => RSExpr::ClosureExpr(RSClosureExpr {ast_node}),
            Expr::ContinueExpr(ContinueExpr) => RSExpr::ContinueExpr(RSContinueExpr {ast_node}),
            Expr::FieldExpr(FieldExpr) => RSExpr::FieldExpr(RSFieldExpr {ast_node}),
            Expr::ForExpr(ForExpr) => RSExpr::ForExpr(RSForExpr {ast_node}),
            Expr::FormatArgsExpr(FormatArgsExpr) => RSExpr::FormatArgsExpr(RSFormatArgsExpr {ast_node}),
            Expr::IfExpr(IfExpr) => RSExpr::IfExpr(RSIfExpr {ast_node}),
            Expr::IndexExpr(IndexExpr) => RSExpr::IndexExpr(RSIndexExpr {ast_node}),
            Expr::LetExpr(LetExpr) => RSExpr::LetExpr(RSLetExpr {ast_node}),
            Expr::Literal(Literal) => RSExpr::Literal(RSLiteral {ast_node}),
            Expr::LoopExpr(LoopExpr) => RSExpr::LoopExpr(RSLoopExpr {ast_node}),
            Expr::MacroExpr(MacroExpr) => RSExpr::MacroExpr(RSMacroExpr {ast_node}),
            Expr::MatchExpr(MatchExpr) => RSExpr::MatchExpr(RSMatchExpr {ast_node}),
            Expr::MethodCallExpr(MethodCallExpr) => RSExpr::MethodCallExpr(RSMethodCallExpr {ast_node}),
            Expr::OffsetOfExpr(OffsetOfExpr) => RSExpr::OffsetOfExpr(RSOffsetOfExpr {ast_node}),
            Expr::ParenExpr(ParenExpr) => RSExpr::ParenExpr(RSParenExpr {ast_node}),
            Expr::PathExpr(PathExpr) => RSExpr::PathExpr(RSPathExpr {ast_node}),
            Expr::PrefixExpr(PrefixExpr) => RSExpr::PrefixExpr(RSPrefixExpr {ast_node}),
            Expr::RangeExpr(RangeExpr) => RSExpr::RangeExpr(RSRangeExpr {ast_node}),
            Expr::RecordExpr(RecordExpr) => RSExpr::RecordExpr(RSRecordExpr {ast_node}),
            Expr::RefExpr(RefExpr) => RSExpr::RefExpr(RSRefExpr {ast_node}),
            Expr::ReturnExpr(ReturnExpr) => RSExpr::ReturnExpr(RSReturnExpr {ast_node}),
            Expr::TryExpr(TryExpr) => RSExpr::TryExpr(RSTryExpr {ast_node}),
            Expr::TupleExpr(TupleExpr) => RSExpr::TupleExpr(RSTupleExpr {ast_node}),
            Expr::UnderscoreExpr(UnderscoreExpr) => RSExpr::UnderscoreExpr(RSUnderscoreExpr {ast_node}),
            Expr::WhileExpr(WhileExpr) => RSExpr::WhileExpr(RSWhileExpr {ast_node}),
            Expr::YeetExpr(YeetExpr) => RSExpr::YeetExpr(RSYeetExpr {ast_node}),
            Expr::YieldExpr(YieldExpr) => RSExpr::YieldExpr(RSYieldExpr {ast_node}),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSArrayExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAwaitExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSBecomeExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSBinExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSBlockExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSBreakExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSCallExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSCastExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSClosureExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSContinueExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSFieldExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSForExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSFormatArgsExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSIfExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSIndexExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLetExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLiteral {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLoopExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMatchExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMethodCallExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSOffsetOfExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSParenExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPathExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPrefixExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRangeExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRecordExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRefExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSReturnExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTryExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTupleExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSUnderscoreExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSWhileExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSYeetExpr {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSYieldExpr {pub(crate) ast_node: RSNode}


#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSStmt {
    ExprStmt(RSExprStmt),
    Item(RSItem),
    LetStmt(RSLetStmt),
}


impl From<Stmt> for RSStmt {
    fn from(stmt: Stmt) -> Self {
        let ast_node = stmt.syntax().into();
        match stmt {
            Stmt::ExprStmt(node) => RSStmt::ExprStmt(RSExprStmt {ast_node}),
            Stmt::Item(node) => RSStmt::Item(node.into()),
            Stmt::LetStmt(node) => RSStmt::LetStmt(RSLetStmt {ast_node}),

        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSExprStmt {pub(crate) ast_node: RSNode}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLetStmt {pub(crate) ast_node: RSNode}


