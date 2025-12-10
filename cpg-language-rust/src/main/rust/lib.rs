uniffi::setup_scaffolding!();

use std::fs;
use ra_ap_syntax::{ast, SourceFile, SyntaxNode};
use ra_ap_syntax::{AstNode, Edition};
use itertools::Itertools;
use ra_ap_syntax::ast::{Adt, ArrayExpr, ArrayType, AsmClobberAbi, AsmConst, AsmExpr, AsmLabel, AsmOperand, AsmOperandNamed, AsmOptions, AsmPiece, AsmRegOperand, AsmSym, AssocItem, AssocTypeArg, AwaitExpr, BecomeExpr, BinExpr, BlockExpr, BoxPat, BreakExpr, CallExpr, CastExpr, ClosureExpr, Const, ConstArg, ConstBlockPat, ConstParam, ContinueExpr, DocCommentIter, DynTraitType, Enum, Expr, ExprStmt, ExternBlock, ExternCrate, ExternItem, FieldExpr, FieldList, Fn, FnPtrType, ForExpr, ForType, FormatArgsExpr, GenericArg, GenericParam, IdentPat, IfExpr, Impl, ImplTraitType, IndexExpr, InferType, Item, LetExpr, LetStmt, Lifetime, LifetimeArg, LifetimeParam, Literal, LiteralPat, LoopExpr, MacroCall, MacroDef, MacroExpr, MacroPat, MacroRules, MacroType, MatchExpr, MethodCallExpr, Module, NameRef, NeverType, OffsetOfExpr, OrPat, ParenExpr, ParenPat, ParenType, Pat, PathExpr, PathPat, PathType, PrefixExpr, PtrType, RangeExpr, RangePat, RecordExpr, RecordFieldList, RecordPat, RefExpr, RefPat, RefType, RestPat, ReturnExpr, SlicePat, SliceType, Static, Stmt, Struct, Trait, TryExpr, TupleExpr, TupleFieldList, TuplePat, TupleStructPat, TupleType, Type, TypeAlias, TypeArg, TypeParam, UnderscoreExpr, Union, Use, UseBoundGenericArg, Variant, VariantDef, WhileExpr, WildcardPat, YeetExpr, YieldExpr};
use crate::RSPat::BoxPat;

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
    end_offset:u32,
    comments: Option<String>
}

impl From<&SyntaxNode> for RSNode {
    fn from(syntax: &SyntaxNode) -> Self {
        let doc_iter = DocCommentIter::from_syntax_node(syntax);
        let docs = itertools::Itertools::join(
            &mut doc_iter.filter_map(|comment| comment.doc_comment().map(ToOwned::to_owned)),
            "\n",
        );
        let comments = if docs.is_empty() { None } else { Some(docs) };

        RSNode {text : syntax.text().to_string(), start_offset: syntax.text_range().start().into(), end_offset: syntax.text_range().end().into(), comments}

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
            Item::Fn(node) => RSItem::Fn(RSFn {ast_node, params: node.param_list().unwrap, ret_type: node.ret_type() }),
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
pub struct RSFn {
    pub(crate) ast_node: RSNode,
    params: Vec<RSPar>,
    ret_type: RSType,
}
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



#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSPat {
    BoxPat(RSBoxPat),
    ConstBlockPat(RSConstBlockPat),
    IdentPat(RSIdentPat),
    LiteralPat(RSLiteralPat),
    MacroPat(RSMacroPat),
    OrPat(RSOrPat),
    ParenPat(RSParenPat),
    PathPat(RSPathPat),
    RangePat(RSRangePat),
    RecordPat(RSRecordPat),
    RefPat(RSRefPat),
    RestPat(RSRestPat),
    SlicePat(RSSlicePat),
    TuplePat(RSTuplePat),
    TupleStructPat(RSTupleStructPat),
    WildcardPat(RSWildcardPat),
}

impl From<Pat> for RSPat {
    fn from(pat: Pat) -> Self {
        let ast_node = pat.syntax().into();
        match pat {
            Pat::BoxPat(node) => RSPat::BoxPat(RSBoxPat {ast_node}),
            Pat::ConstBlockPat(node) => RSPat::ConstBlockPat(RSConstBlockPat {ast_node}),
            Pat::IdentPat(node) => RSPat::IdentPat(RSIdentPat {ast_node}),
            Pat::LiteralPat(node) => RSPat::LiteralPat(RSLiteralPat {ast_node}),
            Pat::MacroPat(node) => RSPat::MacroPat(RSMacroPat {ast_node}),
            Pat::OrPat(node) => RSPat::OrPat(RSOrPat {ast_node}),
            Pat::ParenPat(node) => RSPat::ParenPat(RSParenPat {ast_node}),
            Pat::PathPat(node) => RSPat::PathPat(RSPathPat {ast_node}),
            Pat::RangePat(node) => RSPat::RangePat(RSRangePat {ast_node}),
            Pat::RecordPat(node) => RSPat::RecordPat(RSRecordPat {ast_node}),
            Pat::RefPat(node) => RSPat::RefPat(RSRefPat {ast_node}),
            Pat::RestPat(node) => RSPat::RestPat(RSRestPat {ast_node}),
            Pat::SlicePat(node) => RSPat::SlicePat(RSSlicePat {ast_node}),
            Pat::TuplePat(node) => RSPat::TuplePat(RSTuplePat {ast_node}),
            Pat::TupleStructPat(node) => RSPat::TupleStructPat(RSTupleStructPat {ast_node}),
            Pat::WildcardPat(node) => RSPat::WildcardPat(RSWildcardPat {ast_node}),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSBoxPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSConstBlockPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSIdentPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLiteralPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSOrPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSParenPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPathPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRangePat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRecordPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRefPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRestPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSSlicePat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTuplePat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTupleStructPat {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSWildcardPat {pub(crate) ast_node: RSNode}


#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSType {
    ArrayType(RSArrayType),
    DynTraitType(RSDynTraitType),
    FnPtrType(RSFnPtrType),
    ForType(RSForType),
    ImplTraitType(RSImplTraitType),
    InferType(RSInferType),
    MacroType(RSMacroType),
    NeverType(RSNeverType),
    ParenType(RSParenType),
    PathType(RSPathType),
    PtrType(RSPtrType),
    RefType(RSRefType),
    SliceType(RSSliceType),
    TupleType(RSTupleType),
}

impl From<Type> for RSType {
    fn from(t: Type) -> Self {
        let ast_node = t.syntax().into();
        match t {
            Type::ArrayType(node) => RSType::ArrayType(RSArrayType {ast_node}),
            Type::DynTraitType(node) => RSType::DynTraitType(RSDynTraitType {ast_node}),
            Type::FnPtrType(node) => RSType::FnPtrType(RSFnPtrType {ast_node}),
            Type::ForType(node) => RSType::ForType(RSForType {ast_node}),
            Type::ImplTraitType(node) => RSType::ImplTraitType(RSImplTraitType {ast_node}),
            Type::InferType(node) => RSType::InferType(RSInferType {ast_node}),
            Type::MacroType(node) => RSType::MacroType(RSMacroType {ast_node}),
            Type::NeverType(node) => RSType::NeverType(RSNeverType {ast_node}),
            Type::ParenType(node) => RSType::ParenType(RSParenType {ast_node}),
            Type::PathType(node) => RSType::PathType(RSPathType {ast_node}),
            Type::PtrType(node) => RSType::PtrType(RSPtrType {ast_node}),
            Type::RefType(node) => RSType::RefType(RSRefType {ast_node}),
            Type::SliceType(node) => RSType::SliceType(RSSliceType {ast_node}),
            Type::TupleType(node) => RSType::TupleType(RSTupleType {ast_node}),

        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSArrayType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSDynTraitType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSFnPtrType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSForType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSImplTraitType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSInferType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSNeverType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSParenType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPathType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPtrType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRefType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSSliceType {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTupleType {pub(crate) ast_node: RSNode}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSUseBoundGenericArg {
    Lifetime(RSLifetime),
    NameRef(RSNameRef),
}

impl From<UseBoundGenericArg> for RSUseBoundGenericArg {
    fn from(ubg: UseBoundGenericArg) -> Self {
        let ast_node = ubg.syntax().into();
        match ubg {
            UseBoundGenericArg::Lifetime(node) => RSUseBoundGenericArg::Lifetime(RSLifetime {ast_node}),
            UseBoundGenericArg::NameRef(node) => RSUseBoundGenericArg::NameRef(RSNameRef {ast_node}),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLifetime {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSNameRef {pub(crate) ast_node: RSNode}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSVariantDef {
    Struct(RSStruct),
    Union(RSUnion),
    Variant(RSVariant),
}

impl From<VariantDef> for RSVariantDef {
    fn from(variant: VariantDef) -> Self {
        let ast_node = variant.syntax().into();
        match variant {
            VariantDef::Struct(node) => RSVariantDef::Struct(RSStruct {ast_node}),
            VariantDef::Union(node) => RSVariantDef::Union(RSUnion {ast_node}),
            VariantDef::Variant(node) => RSVariantDef::Variant(RSVariant {ast_node}),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSVariant {pub(crate) ast_node: RSNode}


#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSFieldList {
    RecordFieldList(RSRecordFieldList),
    TupleFieldList(RSTupleFieldList),
}

impl From<FieldList> for RSFieldList {
    fn from(field_list: FieldList) -> Self {
        let ast_node = field_list.syntax().into();
        match field_list {
            FieldList::RecordFieldList(node) => RSFieldList::RecordFieldList(RSRecordFieldList {ast_node}),
            FieldList::TupleFieldList(node) => RSFieldList::TupleFieldList(RSTupleFieldList {ast_node}),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRecordFieldList {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTupleFieldList {pub(crate) ast_node: RSNode}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSGenericArg {
    AssocTypeArg(RSAssocTypeArg),
    ConstArg(RSConstArg),
    LifetimeArg(RSLifetimeArg),
    TypeArg(RSTypeArg),
}

impl From<GenericArg> for RSGenericArg {
    fn from(generic_arg: GenericArg) -> Self {
        let ast_node = generic_arg.syntax().into();
        match generic_arg {
            GenericArg::AssocTypeArg(node) => RSGenericArg::AssocTypeArg(RSAssocTypeArg {ast_node}),
            GenericArg::ConstArg(node) => RSGenericArg::ConstArg(RSConstArg {ast_node}),
            GenericArg::LifetimeArg(node) => RSGenericArg::LifetimeArg(RSLifetimeArg {ast_node}),
            GenericArg::TypeArg(node) => RSGenericArg::TypeArg(RSTypeArg {ast_node}),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAssocTypeArg {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSConstArg {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLifetimeArg {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTypeArg {pub(crate) ast_node: RSNode}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSGenericParam {
    ConstParam(RSConstParam),
    LifetimeParam(RSLifetimeParam),
    TypeParam(RSTypeParam),
}

impl From<GenericParam> for RSGenericParam {
    fn from(generic_param: GenericParam) -> Self {
        let ast_node = generic_param.syntax().into();
        match generic_param {
            GenericParam::ConstParam(node) => RSGenericParam::ConstParam(RSConstParam {ast_node}),
            GenericParam::LifetimeParam(node) => RSGenericParam::LifetimeParam(RSLifetimeParam {ast_node}),
            GenericParam::TypeParam(node) => RSGenericParam::TypeParam(RSTypeParam {ast_node}),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSConstParam {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLifetimeParam {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTypeParam {pub(crate) ast_node: RSNode}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSExternItem {
    Fn(RSFn),
    MacroCall(RSMacroCall),
    Static(RSStatic),
    TypeAlias(RSTypeAlias),
}

impl From<ExternItem> for RSExternItem {
    fn from(ext_item: ExternItem) -> Self {
        let ast_node = ext_item.syntax().into();
        match ext_item {
            ExternItem::Fn(node) => RSExternItem::Fn(RSFn {ast_node}),
            ExternItem::MacroCall(node) => RSExternItem::MacroCall(RSMacroCall {ast_node}),
            ExternItem::Static(node) => RSExternItem::Static(RSStatic {ast_node}),
            ExternItem::TypeAlias(node) => RSExternItem::TypeAlias(RSTypeAlias {ast_node})
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSAsmOperand {
    AsmConst(RSAsmConst),
    AsmLabel(RSAsmLabel),
    AsmRegOperand(RSAsmRegOperand),
    AsmSym(RSAsmSym),
}

impl From<AsmOperand> for RSAsmOperand {
    fn from(asm_op: AsmOperand) -> Self {
        let ast_node = asm_op.syntax().into();
        match asm_op {
            AsmOperand::AsmConst(node) => RSAsmOperand::AsmConst(RSAsmConst {ast_node}),
            AsmOperand::AsmLabel(node) => RSAsmOperand::AsmLabel(RSAsmLabel {ast_node}),
            AsmOperand::AsmRegOperand(node) => RSAsmOperand::AsmRegOperand(RSAsmRegOperand {ast_node}),
            AsmOperand::AsmSym(node) => RSAsmOperand::AsmSym(RSAsmSym {ast_node}),
        }
    }
}


#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmConst {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmLabel {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmRegOperand {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmSym {pub(crate) ast_node: RSNode}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSAsmPiece {
    AsmClobberAbi(RSAsmClobberAbi),
    AsmOperandNamed(RSAsmOperandNamed),
    AsmOptions(RSAsmOptions),
}

impl From<AsmPiece> for RSAsmPiece {
    fn from(asm_p: AsmPiece) -> Self {
        let ast_node = asm_p.syntax().into();
        match asm_p {
            AsmPiece::AsmClobberAbi(node) => RSAsmPiece::AsmClobberAbi(RSAsmClobberAbi {ast_node}),
            AsmPiece::AsmOperandNamed(node) => RSAsmPiece::AsmOperandNamed(RSAsmOperandNamed {ast_node}),
            AsmPiece::AsmOptions(node) => RSAsmPiece::AsmOptions(RSAsmOptions {ast_node}),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmClobberAbi {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmOperandNamed {pub(crate) ast_node: RSNode}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmOptions {pub(crate) ast_node: RSNode}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSAssocItem {
    Const(RSConst),
    Fn(RSFn),
    MacroCall(RSMacroCall),
    TypeAlias(RSTypeAlias),
}

impl From<AssocItem> for RSAssocItem {
    fn from(assoc_item: AssocItem) -> Self {
        let ast_node = assoc_item.syntax().into();
        match assoc_item {
            AssocItem::Const(node) => RSAssocItem::Const(RSConst {ast_node}),
            AssocItem::Fn(node) => RSAssocItem::Fn(RSFn {ast_node})
            AssocItem::MacroCall(node) => RSAssocItem::MacroCall(RSMacroCall {ast_node}),
            AssocItem::TypeAlias(node) => RSAssocItem::TypeAlias(RSTypeAlias {ast_node}),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSAdt {
    Enum(RSEnum),
    Struct(RSStruct),
    Union(RSUnion),
}

impl From<Adt> for RSAdt {
    fn from(adt: Adt) -> Self {
        let ast_node = adt.syntax().into();
        match adt {
            Adt::Enum(node) => RSAdt::Enum(RSEnum {ast_node}),
            Adt::Struct(node) => RSAdt::Struct(RSStruct {ast_node}),
            Adt::Union(node) => RSAdt::Union(RSUnion {ast_node}),
        }
    }
}


