uniffi::setup_scaffolding!();

use std::fs;
use ra_ap_syntax::{ast, SourceFile, SyntaxNode, SyntaxToken};
use ra_ap_syntax::{AstNode, Edition};
use itertools::Itertools;
use ra_ap_parser::{SyntaxKind, T};
use ra_ap_syntax::ast::{Abi, Adt, ArgList, ArrayExpr, ArrayType, AsmClobberAbi, AsmConst, AsmExpr, AsmLabel, AsmOperand, AsmOperandNamed, AsmOptions, AsmPiece, AsmRegOperand, AsmSym, AssocItem, AssocTypeArg, AwaitExpr, BecomeExpr, BinExpr, BlockExpr, BoxPat, BreakExpr, CallExpr, CastExpr, ClosureExpr, Const, ConstArg, ConstBlockPat, ConstParam, ContinueExpr, DocCommentIter, DynTraitType, Enum, Expr, ExprStmt, ExternBlock, ExternCrate, ExternItem, FieldExpr, FieldList, Fn, FnPtrType, ForExpr, ForType, FormatArgsExpr, GenericArg, GenericParam, HasArgList, HasGenericArgs, HasGenericParams, HasLoopBody, HasName, HasTypeBounds, IdentPat, IfExpr, Impl, ImplTraitType, IndexExpr, InferType, Item, LetElse, LetExpr, LetStmt, Lifetime, LifetimeArg, LifetimeParam, Literal, LiteralPat, LoopExpr, MacroCall, MacroDef, MacroExpr, MacroPat, MacroRules, MacroType, MatchArm, MatchExpr, MethodCallExpr, Module, NameRef, NeverType, OffsetOfExpr, OrPat, Param, ParamList, ParenExpr, ParenPat, ParenType, Pat, Path, PathExpr, PathPat, PathSegment, PathType, PrefixExpr, PtrType, RangeExpr, RangePat, RecordExpr, RecordExprField, RecordField, RecordFieldList, RecordPat, RecordPatField, RefExpr, RefPat, RefType, RestPat, RetType, ReturnExpr, ReturnTypeSyntax, SelfParam, SlicePat, SliceType, Static, Stmt, Struct, TokenTree, Trait, TryExpr, TupleExpr, TupleField, TupleFieldList, TuplePat, TupleStructPat, TupleType, Type, TypeAlias, TypeAnchor, TypeArg, TypeBound, TypeBoundList, TypeParam, UnderscoreExpr, Union, Use, UseBoundGenericArg, UseTree, Variant, VariantDef, WhileExpr, WildcardPat, YeetExpr, YieldExpr};
use crate::RSAst::RustProblem;
use ra_ap_syntax::ast::HasModuleItem;

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
    comments: Option<String>,
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

/// Creating a common root node for all AST nodes
#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSAst {
    RustItem(RSItem),
    RustExpr(RSExpr),
    RustStmt(RSStmt),
    RustType(RSType), // Represent mentions of a type in the code
    RustAbi(RSAbi), // Needed for now to have Abi nodes in the hierarchy
    RustProblem(RSProblem), // Used to represent nodes that we are currently not making an interface for
    RustUseTree(RSUseTree),
    RustPat(RSPat),
    RustVariant(RSVariant) // Added as ast element to have it in the hierarchy
}

impl From<SyntaxNode> for RSAst {
    fn from(syntax: SyntaxNode) -> Self {
        let kind = syntax.kind();
        if let Some(rnode) = Abi::cast(syntax.clone_subtree()) {
            return RSAst::RustAbi(rnode.into())
        }
        if let Some(rnode) = AsmExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::AsmExpr(rnode).into())
        }
        if let Some(rnode) = Const::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::Const(rnode).into())
        }
        if let Some(rnode) = Enum::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::Enum(rnode).into())
        }
        if let Some(rnode) = ExternBlock::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::ExternBlock(rnode).into())
        }
        if let Some(rnode) = ExternCrate::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::ExternCrate(rnode).into())
        }
        if let Some(rnode) = Fn::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::Fn(rnode).into())
        }
        if let Some(rnode) = Impl::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::Impl(rnode).into())
        }
        if let Some(rnode) = MacroCall::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::MacroCall(rnode).into())
        }
        if let Some(rnode) = MacroDef::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::MacroDef(rnode).into())
        }
        if let Some(rnode) = MacroRules::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::MacroRules(rnode).into())
        }
        if let Some(rnode) = Module::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::Module(rnode).into())
        }
        if let Some(rnode) = Static::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::Static(rnode).into())
        }
        if let Some(rnode) = Struct::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::Struct(rnode).into())
        }
        if let Some(rnode) = Trait::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::Trait(rnode).into())
        }
        if let Some(rnode) = TypeAlias::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::TypeAlias(rnode).into())
        }
        if let Some(rnode) = Union::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::Union(rnode).into())
        }
        if let Some(rnode) = Use::cast(syntax.clone_subtree()) {
            return RSAst::RustItem(Item::Use(rnode).into())
        }
        if let Some(rnode) = ArrayExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::ArrayExpr(rnode).into())
        }
        if let Some(rnode) = AsmExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::AsmExpr(rnode).into())
        }
        if let Some(rnode) = AwaitExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::AwaitExpr(rnode).into())
        }
        if let Some(rnode) = BecomeExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::BecomeExpr(rnode).into())
        }
        if let Some(rnode) = BinExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::BinExpr(rnode).into())
        }
        if let Some(rnode) = BlockExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::BlockExpr(rnode).into())
        }
        if let Some(rnode) = BreakExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::BreakExpr(rnode).into())
        }
        if let Some(rnode) = CastExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::CastExpr(rnode).into())
        }
        if let Some(rnode) = ClosureExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::ClosureExpr(rnode).into())
        }
        if let Some(rnode) = ContinueExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::ContinueExpr(rnode).into())
        }
        if let Some(rnode) = FieldExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::FieldExpr(rnode).into())
        }
        if let Some(rnode) = ForExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::ForExpr(rnode).into())
        }
        if let Some(rnode) = FormatArgsExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::FormatArgsExpr(rnode).into())
        }
        if let Some(rnode) = IfExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::IfExpr(rnode).into())
        }
        if let Some(rnode) = IndexExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::IndexExpr(rnode).into())
        }
        if let Some(rnode) = LetExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::LetExpr(rnode).into())
        }
        if let Some(rnode) = Literal::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::Literal(rnode).into())
        }
        if let Some(rnode) = LoopExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::LoopExpr(rnode).into())
        }
        if let Some(rnode) = MacroExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::MacroExpr(rnode).into())
        }
        if let Some(rnode) = MatchExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::MatchExpr(rnode).into())
        }
        if let Some(rnode) = MethodCallExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::MethodCallExpr(rnode).into())
        }
        if let Some(rnode) = OffsetOfExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::OffsetOfExpr(rnode).into())
        }
        if let Some(rnode) = ParenExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::ParenExpr(rnode).into())
        }
        if let Some(rnode) = PathExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::PathExpr(rnode).into())
        }
        if let Some(rnode) = PrefixExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::PrefixExpr(rnode).into())
        }
        if let Some(rnode) = RangeExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::RangeExpr(rnode).into())
        }
        if let Some(rnode) = RecordExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::RecordExpr(rnode).into())
        }
        if let Some(rnode) = RefExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::RefExpr(rnode).into())
        }
        if let Some(rnode) = ReturnExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::ReturnExpr(rnode).into())
        }
        if let Some(rnode) = TryExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::TryExpr(rnode).into())
        }
        if let Some(rnode) = TupleExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::TupleExpr(rnode).into())
        }
        if let Some(rnode) = UnderscoreExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::UnderscoreExpr(rnode).into())
        }
        if let Some(rnode) = WhileExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::WhileExpr(rnode).into())
        }
        if let Some(rnode) = YeetExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::YeetExpr(rnode).into())
        }
        if let Some(rnode) = YieldExpr::cast(syntax.clone_subtree()) {
            return RSAst::RustExpr(Expr::YieldExpr(rnode).into())
        }

        println!("Not correctly translating node of type: {}", kind.text().to_string());
        RustProblem(syntax.into())
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSProblem {pub(crate) ast_node: RSNode}
impl From<SyntaxNode> for RSProblem {
    fn from(syntax:  SyntaxNode) -> Self {RSProblem{ast_node: (&syntax).into()}}
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
    // Defined by us
    Param(RSParam),
    SelfParam(RSSelfParam)
}



impl From<Item> for RSItem {
    fn from(item: Item) -> Self {
        match item {
            Item::AsmExpr(node) => RSItem::AsmExpr(node.into()),
            Item::Const(node) => RSItem::Const(node.into()),
            Item::Enum(node) => RSItem::Enum(node.into()),
            Item::ExternBlock(node) => RSItem::ExternBlock(node.into()),
            Item::ExternCrate(node) => RSItem::ExternCrate(node.into()),
            Item::Fn(node) => RSItem::Fn(node.into()),
            Item::Impl(node) => RSItem::Impl(node.into()),
            Item::MacroCall(node) => RSItem::MacroCall(node.into()),
            Item::MacroDef(node) => RSItem::MacroDef(node.into()),
            Item::MacroRules(node) => RSItem::MacroRules(node.into()),
            Item::Module(node) => RSItem::Module(node.into()),
            Item::Static(node) => RSItem::Static(node.into()),
            Item::Struct(node) => RSItem::Struct(node.into()),
            Item::Trait(node) => RSItem::Trait(node.into()),
            Item::TypeAlias(node) => RSItem::TypeAlias(node.into()),
            Item::Union(node) => RSItem::Union(node.into()),
            Item::Use(node) => RSItem::Use(node.into()),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmExpr {pub(crate) ast_node: RSNode}
impl From<AsmExpr> for RSAsmExpr {
    fn from(node:  AsmExpr) -> Self {RSAsmExpr{ast_node: node.syntax().into()}}
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSConst {
    pub(crate) ast_node: RSNode,
    name: Option<String>,
    ty: Option<RSType>,
    expr: Vec<RSExpr>,
    generic_params: Vec<RSGenericParam>
}
impl From<Const> for RSConst {
    fn from(node:  Const) -> Self {
        RSConst{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string()),
            ty: node.ty().map(Into::into),
            expr: node.syntax().children().find_map(Expr::cast).map(Into::into).into_iter().collect(),
            generic_params: node.generic_param_list().map(|gpl|gpl.generic_params().into_iter().map(Into::into)).into_iter().flatten().collect_vec()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSEnum {
    pub(crate) ast_node: RSNode,
    name: Option<String>,
    variants: Vec<RSVariant>,
    generic_params: Vec<RSGenericParam>

}
impl From<Enum> for RSEnum {
    fn from(node:  Enum) -> Self {
        RSEnum{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string()),
            variants: node.variant_list().map(|vl|vl.variants().into_iter().map(Into::into)).into_iter().flatten().collect_vec(),
            generic_params: node.generic_param_list().map(|gpl|gpl.generic_params().into_iter().map(Into::into)).into_iter().flatten().collect_vec()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSExternBlock {pub(crate) ast_node: RSNode, extern_items: Vec<RSExternItem>, abi: Option<RSAbi>}
impl From<ExternBlock> for RSExternBlock {
    fn from(node:  ExternBlock) -> Self {
        RSExternBlock{
            ast_node: node.syntax().into(),
            abi: node.abi().map(Into::into),
            extern_items: node.extern_item_list().map(|eil|eil.extern_items().map(Into::into).collect::<Vec<_>>()).unwrap_or_default()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSExternCrate {pub(crate) ast_node: RSNode, name_ref: Option<RSNameRef>, rename: Option<String>}
impl From<ExternCrate> for RSExternCrate {
    fn from(node:  ExternCrate) -> Self {
        RSExternCrate{
            ast_node: node.syntax().into(),
            name_ref: node.name_ref().map(Into::into),
            rename: node.rename().map(|rn|rn.name().map(|n|n.to_string())).unwrap_or_default(),
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSFn {
    pub(crate) ast_node: RSNode,
    pub param_list: Option<RSParamList>,
    pub ret_type: Option<RSType>,
    pub body: Option<RSBlockExpr>,
    name: Option<String>,
    generic_params: Vec<RSGenericParam>
}
impl From<Fn> for RSFn {
    fn from(node:  Fn) -> Self {

        RSFn{
            ast_node: node.syntax().into(),
            param_list: node.param_list().map(Into::into),
            ret_type: node.ret_type().map(|o|o.ty().map(Into::into)).flatten(),
            body: node.syntax().children().find_map(BlockExpr::cast).map(Into::into),
            name: node.name().map(|n|n.to_string()),
            generic_params: node.generic_param_list().map(|gpl|gpl.generic_params().into_iter().map(Into::into)).into_iter().flatten().collect_vec()
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAbi {pub(crate) ast_node: RSNode, has_extern: bool, string_literal: String}
impl From<Abi> for RSAbi {
    fn from(node:  Abi) -> Self {

        RSAbi{
            ast_node: node.syntax().into(),
            has_extern: node.extern_token().is_some(),
            string_literal: node.syntax().text().to_string()
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSImpl {pub(crate) ast_node: RSNode, items: Vec<RSAssocItem>, path_types: Vec<RSPathType>, generic_params: Vec<RSGenericParam>}
impl From<Impl> for RSImpl {
    fn from(node:  Impl) -> Self {
        RSImpl{
            ast_node: node.syntax().into(),
            items: node.assoc_item_list().map(|ail|ail.assoc_items().map(Into::into).collect::<Vec<_>>()).unwrap_or_default(),
            path_types: node.syntax().children().filter_map(PathType::cast).map(Into::into).collect::<Vec<_>>(),
            generic_params: node.generic_param_list().map(|gpl|gpl.generic_params().into_iter().map(Into::into)).into_iter().flatten().collect_vec()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroCall {pub(crate) ast_node: RSNode, path: Option<RSPath>, macro_string: String}
impl From<MacroCall> for RSMacroCall {
    fn from(node:  MacroCall) -> Self {RSMacroCall{
        ast_node: node.syntax().into(),
        path: node.path().map(Into::into),
        macro_string: node.token_tree().map(|s|s.to_string()).unwrap_or_default()
    }}
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroDef {pub(crate) ast_node: RSNode, name: Option<String>}
impl From<MacroDef> for RSMacroDef {
    fn from(node:  MacroDef) -> Self {
        RSMacroDef{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string())
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroRules {pub(crate) ast_node: RSNode, name: Option<String>}
impl From<MacroRules> for RSMacroRules {
    fn from(node: MacroRules) -> Self {
        RSMacroRules{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string())
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSModule {pub(crate) ast_node: RSNode, name: Option<String>, items: Vec<RSItem>}
impl From<Module> for RSModule {
    fn from(node:  Module) -> Self {

        RSModule{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string()),
            items: node.item_list().map(|il| il.items().map(Into::into).collect::<Vec<_>>()).unwrap_or_default()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSStatic {
    pub(crate) ast_node: RSNode, name: Option<String>, ty: Option<RSType>
}
impl From<Static> for RSStatic {
    fn from(node:  Static) -> Self {
        RSStatic{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string()),
            ty: node.ty().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSStruct {
    pub(crate) ast_node: RSNode,
    name: Option<String>,
    field_list: Option<RSFieldList>,
    generic_params: Vec<RSGenericParam>
}
impl From<Struct> for RSStruct {
    fn from(node:  Struct) -> Self {
        RSStruct{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string()),
            field_list: node.field_list().map(Into::into),
            generic_params: node.generic_param_list().map(|gpl|gpl.generic_params().into_iter().map(Into::into)).into_iter().flatten().collect_vec()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTrait {
    pub(crate) ast_node: RSNode,
    name: Option<String>,
    items: Vec<RSAssocItem>,
    generic_params: Vec<RSGenericParam>
}
impl From<Trait> for RSTrait {
    fn from(node:  Trait) -> Self {
        RSTrait{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string()),
            items: node.assoc_item_list().map(|ail|ail.assoc_items().map(Into::into).collect::<Vec<_>>()).unwrap_or_default(),
            generic_params: node.generic_param_list().map(|gpl|gpl.generic_params().into_iter().map(Into::into)).into_iter().flatten().collect_vec()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTypeAlias {pub(crate) ast_node: RSNode, name: Option<String>,ty: Option<RSType>, type_bound_list: Vec<RSTypeBound>, generic_params: Vec<RSGenericParam>}
impl From<TypeAlias> for RSTypeAlias {
    fn from(node:  TypeAlias) -> Self {
        RSTypeAlias{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string()),
            ty: node.ty().map(Into::into),
            type_bound_list: node.type_bound_list().iter().flat_map(|tb|tb.bounds().map(Into::into)).collect::<Vec<_>>(),
            generic_params: node.generic_param_list().map(|gpl|gpl.generic_params().into_iter().map(Into::into)).into_iter().flatten().collect_vec()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSUnion {
    pub(crate) ast_node: RSNode,
    generic_params: Vec<RSGenericParam>,
    name: Option<String>,
    field_list: Option<RSRecordFieldList>
}
impl From<Union> for RSUnion {
    fn from(node:  Union) -> Self {
        RSUnion{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string()),
            field_list: node.record_field_list().map(Into::into),
            generic_params: node.generic_param_list().map(|gpl|gpl.generic_params().into_iter().map(Into::into)).into_iter().flatten().collect_vec()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSUse {pub(crate) ast_node: RSNode, use_tree: Option<RSUseTree>}
impl From<Use> for RSUse {
    fn from(node:  Use) -> Self {
        RSUse{
            ast_node: node.syntax().into(),
            use_tree: node.use_tree().map(Into::into)
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSUseTree {pub(crate) ast_node: RSNode, path: Option<RSPath>, rename: Option<String>, use_trees: Vec<RSUseTree>, star: bool}
impl From<UseTree> for RSUseTree {
    fn from(node:  UseTree) -> Self {
        RSUseTree{
            ast_node: node.syntax().into(),
            path: node.path().map(Into::into),
            rename: node.rename().map(|rn|rn.name().map(|n|n.to_string())).unwrap_or_default(),
            use_trees: node.use_tree_list().map(|utl|utl.use_trees().map(Into::into).collect::<Vec<_>>()).unwrap_or_default(),
            star: node.star_token().is_some()
        }
    }
}



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
    MatchArm(RSMatchArm),
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
    Path(RSPath),
    PathSegment(RSPathSegment),
    NameRef(RSNameRef),
    RecordExprField(RSRecordExprField)
}

impl From<Expr> for RSExpr {
    fn from(expr: Expr) -> Self {
        match expr {
            Expr::ArrayExpr(node) => RSExpr::ArrayExpr(node.into()),
            Expr::AsmExpr(node) => RSExpr::AsmExpr(node.into()),
            Expr::AwaitExpr(node) => RSExpr::AwaitExpr(node.into()),
            Expr::BecomeExpr(node) => RSExpr::BecomeExpr(node.into()),
            Expr::BinExpr(node) => RSExpr::BinExpr(node.into()),
            Expr::BlockExpr(node) => RSExpr::BlockExpr(node.into()),
            Expr::BreakExpr(node) => RSExpr::BreakExpr(node.into()),
            Expr::CallExpr(node) => RSExpr::CallExpr(node.into()),
            Expr::CastExpr(node) => RSExpr::CastExpr(node.into()),
            Expr::ClosureExpr(node) => RSExpr::ClosureExpr(node.into()),
            Expr::ContinueExpr(node) => RSExpr::ContinueExpr(node.into()),
            Expr::FieldExpr(node) => RSExpr::FieldExpr(node.into()),
            Expr::ForExpr(node) => RSExpr::ForExpr(node.into()),
            Expr::FormatArgsExpr(node) => RSExpr::FormatArgsExpr(node.into()),
            Expr::IfExpr(node) => RSExpr::IfExpr(node.into()),
            Expr::IndexExpr(node) => RSExpr::IndexExpr(node.into()),
            Expr::LetExpr(node) => RSExpr::LetExpr(node.into()),
            Expr::Literal(node) => RSExpr::Literal(node.into()),
            Expr::LoopExpr(node) => RSExpr::LoopExpr(node.into()),
            Expr::MacroExpr(node) => RSExpr::MacroExpr(node.into()),
            Expr::MatchExpr(node) => RSExpr::MatchExpr(node.into()),
            Expr::MethodCallExpr(node) => RSExpr::MethodCallExpr(node.into()),
            Expr::OffsetOfExpr(node) => RSExpr::OffsetOfExpr(node.into()),
            Expr::ParenExpr(node) => RSExpr::ParenExpr(node.into()),
            Expr::PathExpr(node) => RSExpr::PathExpr(node.into()),
            Expr::PrefixExpr(node) => RSExpr::PrefixExpr(node.into()),
            Expr::RangeExpr(node) => RSExpr::RangeExpr(node.into()),
            Expr::RecordExpr(node) => RSExpr::RecordExpr(node.into()),
            Expr::RefExpr(node) => RSExpr::RefExpr(node.into()),
            Expr::ReturnExpr(node) => RSExpr::ReturnExpr(node.into()),
            Expr::TryExpr(node) => RSExpr::TryExpr(node.into()),
            Expr::TupleExpr(node) => RSExpr::TupleExpr(node.into()),
            Expr::UnderscoreExpr(node) => RSExpr::UnderscoreExpr(node.into()),
            Expr::WhileExpr(node) => RSExpr::WhileExpr(node.into()),
            Expr::YeetExpr(node) => RSExpr::YeetExpr(node.into()),
            Expr::YieldExpr(node) => RSExpr::YieldExpr(node.into()),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSArrayExpr {pub(crate) ast_node: RSNode, expressions: Vec<RSExpr>, repeating: bool}
impl From<ArrayExpr> for RSArrayExpr {
    fn from(node:  ArrayExpr) -> Self {
        RSArrayExpr{
            ast_node: node.syntax().into(),
            expressions: node.exprs().into_iter().map(Into::into).collect(),
            repeating : node.semicolon_token().is_some()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAwaitExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>}
impl From<AwaitExpr> for RSAwaitExpr {
    fn from(node: AwaitExpr ) -> Self {
        RSAwaitExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSBecomeExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>}
impl From<BecomeExpr> for RSBecomeExpr {
    fn from(node:  BecomeExpr) -> Self {
        RSBecomeExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSBinExpr {
    pub(crate) ast_node: RSNode,
    expressions: Vec<RSExpr>,
    operator: String,
}
impl From<BinExpr> for RSBinExpr {
    fn from(node: BinExpr ) -> Self {
        RSBinExpr{
            ast_node: node.syntax().into(),
            expressions: node.syntax().children().filter_map(Expr::cast).map(Into::into)
                .collect(),
            operator: node.syntax().children_with_tokens().filter(|sn|sn.kind().is_punct()).map(|p|p.to_string()).into_iter().collect()
        }
    }
}


#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSBlockExpr {
    pub(crate) ast_node: RSNode,
    pub stmts: Vec<RSStmt>,
    pub tail_expr: Vec<RSExpr>,
}
impl From<BlockExpr> for RSBlockExpr {
    fn from(node:  BlockExpr) -> Self {
        let ret = RSBlockExpr{
            ast_node: node.syntax().into(),
            stmts: node.stmt_list().map(|sl| sl.statements().map(Into::into).collect::<Vec<_>>()).unwrap_or_default(),
            tail_expr: node.stmt_list().map(|sl|sl.tail_expr().map(Into::into)).flatten().into_iter().collect()
        };
        ret
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSBreakExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>, lifetime: Option<RSLifetime>}
impl From<BreakExpr> for RSBreakExpr {
    fn from(node:  BreakExpr) -> Self {
        RSBreakExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect(),
            lifetime: node.lifetime().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSCallExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>, arguments: Vec<RSExpr>}
impl From<CallExpr> for RSCallExpr {
    fn from(node:  CallExpr) -> Self {
        RSCallExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect(),
            arguments: node
                .arg_list()
                .into_iter()
                .flat_map(|a| a.syntax().children().filter_map(Expr::cast).map(Into::into))
                .collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSCastExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>, ty: Vec<RSType>}
impl From<CastExpr> for RSCastExpr {
    fn from(node:  CastExpr) -> Self {
        RSCastExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect(),
            ty: node.ty().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSClosureExpr {pub(crate) ast_node: RSNode, param_list: Vec<RSParamList>, ret_type: Vec<RSType>, expressions: Vec<RSExpr>}
impl From<ClosureExpr> for RSClosureExpr {
    fn from(node:  ClosureExpr) -> Self {
        RSClosureExpr{
            ast_node: node.syntax().into(),
            param_list: node.param_list().map(Into::into).into_iter().collect::<Vec<_>>(),
            ret_type: node.ret_type().and_then(|rt|rt.ty().map(Into::into)).into_iter().collect::<Vec<_>>(),
            expressions: node.syntax().children().filter_map(Expr::cast).map(Into::into)
                .collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSContinueExpr {pub(crate) ast_node: RSNode, lifetime: Option<RSLifetime>}
impl From<ContinueExpr> for RSContinueExpr {
    fn from(node:  ContinueExpr) -> Self {
        RSContinueExpr{
            ast_node: node.syntax().into(),
            lifetime: node.lifetime().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSFieldExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>, name_ref: Option<RSNameRef>}
impl From<FieldExpr> for RSFieldExpr {
    fn from(node:  FieldExpr) -> Self {
        RSFieldExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect(),
            name_ref: node.name_ref().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSForExpr {pub(crate) ast_node: RSNode, pat: Option<RSPat>, expressions: Vec<RSExpr>}
impl From<ForExpr> for RSForExpr {
    fn from(node:  ForExpr) -> Self {
        RSForExpr{
            ast_node: node.syntax().into(),
            pat: node.pat().map(Into::into),
            expressions: node.syntax().children().filter_map(Expr::cast).map(Into::into)
                .collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSFormatArgsExpr {
    pub(crate) ast_node: RSNode,
    template: Vec<RSExpr>,
    has_pound: bool,
    has_comma: bool,
    has_builtin: bool,
    has_format_args: bool
}
impl From<FormatArgsExpr> for RSFormatArgsExpr {
    fn from(node: FormatArgsExpr ) -> Self {
        RSFormatArgsExpr{
            ast_node: node.syntax().into(),
            template: node.template().map(Into::into).into_iter().collect(),
            has_pound: node.template().is_some(),
            has_comma: node.template().is_some(),
            has_builtin: node.template().is_some(),
            has_format_args: node.template().is_some()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSIfExpr {pub(crate) ast_node: RSNode, expressions: Vec<RSExpr>}
impl From<IfExpr> for RSIfExpr {
    fn from(node: IfExpr ) -> Self {
        RSIfExpr{
            ast_node: node.syntax().into(),
            expressions: node.syntax().children().filter_map(Expr::cast).map(Into::into)
                .collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSIndexExpr {pub(crate) ast_node: RSNode, expressions: Vec<RSExpr>}
impl From<IndexExpr> for RSIndexExpr {
    fn from(node:  IndexExpr) -> Self {
        RSIndexExpr{
            ast_node: node.syntax().into(),
            expressions: node.syntax().children().filter_map(Expr::cast).map(Into::into)
                .collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLetExpr {pub(crate) ast_node: RSNode, pub expr: Vec<RSExpr>, pub pat: Vec<RSPat>}
impl From<LetExpr> for RSLetExpr {
    fn from(node:  LetExpr) -> Self {
        RSLetExpr{ast_node: node.syntax().into(), expr: node.expr().map(Into::into).into_iter().collect(), pat: node.pat().map(Into::into).into_iter().collect()}
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLiteral {pub(crate) ast_node: RSNode, literal_type: RSLiteralType}
impl From<Literal> for RSLiteral {
    fn from(node:  Literal) -> Self {

        let mut kind = RSLiteralType::UnknownL;
        for literalKind in node.syntax().children().map(|n|n.kind()).filter(|k|k.is_literal()) {

            kind = match literalKind {
                SyntaxKind::BYTE => RSLiteralType::ByteL,
                SyntaxKind::BYTE_STRING => RSLiteralType::ByteStringL,
                SyntaxKind::CHAR => RSLiteralType::CharL,
                SyntaxKind::C_STRING => RSLiteralType::CStringL,
                SyntaxKind::FLOAT_NUMBER => RSLiteralType::FloatNumberL,
                SyntaxKind::INT_NUMBER => RSLiteralType::IntNumberL,
                SyntaxKind::STRING => RSLiteralType::StringL,
                _ => RSLiteralType::UnknownL
            }
        }
        RSLiteral{ast_node: node.syntax().into(), literal_type: kind}
    }
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
enum RSLiteralType {
    ByteL,
    ByteStringL,
    CharL,
    CStringL,
    FloatNumberL,
    IntNumberL,
    StringL,
    UnknownL,
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLoopExpr {pub(crate) ast_node: RSNode, label: Option<String>, body: Vec<RSBlockExpr>}
impl From<LoopExpr> for RSLoopExpr {
    fn from(node:  LoopExpr) -> Self {
        RSLoopExpr{
            ast_node: node.syntax().into(),
            label: node.label().map(|l|l.to_string()),
            body: node.loop_body().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroExpr {pub(crate) ast_node: RSNode, macro_call: Option<RSMacroCall>}
impl From<MacroExpr> for RSMacroExpr {
    fn from(node:  MacroExpr) -> Self {
        RSMacroExpr{
            ast_node: node.syntax().into(),
            macro_call: node.macro_call().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMatchExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>, arms: Vec<RSMatchArm>}
impl From<MatchExpr> for RSMatchExpr {
    fn from(node:  MatchExpr) -> Self {
        RSMatchExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect(),
            arms: node.match_arm_list().map(|mal| mal.arms().map(Into::into).collect()).unwrap_or_default()
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMatchArm {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>, pat: Vec<RSPat>, guard: Vec<RSExpr>}
impl From<MatchArm> for RSMatchArm {
    fn from(node:  MatchArm) -> Self {
        RSMatchArm{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect(),
            pat: node.pat().map(Into::into).into_iter().collect(),
            guard: node.guard().map(|g|g.syntax().children().filter_map(Expr::cast).next().map(Into::into).into_iter().collect()).unwrap_or_default()
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMethodCallExpr {pub(crate) ast_node: RSNode, receiver: Vec<RSExpr>, name_ref: Option<RSNameRef>, arguments: Vec<RSExpr>}
impl From<MethodCallExpr> for RSMethodCallExpr {
    fn from(node:  MethodCallExpr) -> Self {
        RSMethodCallExpr{
            ast_node: node.syntax().into(),
            receiver: node.receiver().map(Into::into).into_iter().collect(),
            name_ref: node.name_ref().map(Into::into),
            arguments: node
                .arg_list()
                .into_iter()
                .flat_map(|a| a.syntax().children().filter_map(Expr::cast).map(Into::into))
                .collect()

        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSOffsetOfExpr {
    pub(crate) ast_node: RSNode,
    fields: Vec<RSNameRef>,
    ty: Vec<RSType>
}
impl From<OffsetOfExpr> for RSOffsetOfExpr {
    fn from(node: OffsetOfExpr ) -> Self {
        RSOffsetOfExpr{
            ast_node: node.syntax().into(),
            fields: node.fields().map(|f|f.into()).into_iter().collect(),
            ty: node.ty().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSParenExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>}
impl From<ParenExpr> for RSParenExpr {
    fn from(node:  ParenExpr) -> Self {
        RSParenExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPathExpr {pub(crate) ast_node: RSNode, pub path: Option<RSPath>}
impl From<PathExpr> for RSPathExpr {
    fn from(node:  PathExpr) -> Self {RSPathExpr{ast_node: node.syntax().into(), path: node.path().map(Into::into)}}
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPathSegment {
    pub(crate) ast_node: RSNode,
    name_ref: Option<RSNameRef>,
    type_args: Vec<RSType>,
    ret_type: Vec<RSType>,
    ret_type_syntax: Option<RSReturnTypeSyntax>,
    ty_anchor: Option<RSTypeAnchor>
}
impl From<PathSegment> for RSPathSegment {
    fn from(node:  PathSegment) -> Self {
        RSPathSegment{
            ast_node: node.syntax().into(),
            name_ref: node.name_ref().map(Into::into),
            type_args: node.parenthesized_arg_list().map(|pal| {
                    pal.type_args().filter_map(|ta| ta.ty().map(Into::into)).collect::<Vec<_>>()
                }).unwrap_or_default(),
            ret_type: node.ret_type().map(|rty|rty.ty().map(Into::into)).flatten().into_iter().collect(),
            ret_type_syntax: node.return_type_syntax().map(Into::into),
            ty_anchor: node.type_anchor().map(Into::into)
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSReturnTypeSyntax {pub(crate) ast_node: RSNode, l_paren: bool, r_paren: bool, dotdot: bool}
impl From<ReturnTypeSyntax> for RSReturnTypeSyntax {
    fn from(node:  ReturnTypeSyntax) -> Self {
        RSReturnTypeSyntax{
            ast_node: node.syntax().into(),
            l_paren: node.l_paren_token().is_some(),
            r_paren: node.r_paren_token().is_some(),
            dotdot: node.dotdot_token().is_some(),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTypeAnchor {pub(crate) ast_node: RSNode, path_ty: Vec<RSPathType>, ty: Vec<RSType>}
impl From<TypeAnchor> for RSTypeAnchor {
    fn from(node:  TypeAnchor) -> Self {
        RSTypeAnchor{
            ast_node: node.syntax().into(),
            path_ty: node.path_type().map(Into::into).into_iter().collect(),
            ty: node.ty().map(Into::into).into_iter().collect(),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPrefixExpr {pub(crate) ast_node: RSNode, operator: String, expr: Vec<RSExpr>}
impl From<PrefixExpr> for RSPrefixExpr {
    fn from(node: PrefixExpr ) -> Self {
        RSPrefixExpr{
            ast_node: node.syntax().into(),
            operator: node.syntax().children_with_tokens().filter(|sn|sn.kind().is_punct()).map(|p|p.to_string()).into_iter().collect(),
            expr: node.expr().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRangeExpr {pub(crate) ast_node: RSNode, expressions: Vec<RSExpr>, operator: String}
impl From<RangeExpr> for RSRangeExpr {
    fn from(node: RangeExpr ) -> Self {
        RSRangeExpr{
            ast_node: node.syntax().into(),
            operator: node.syntax().children_with_tokens().filter(|sn|sn.kind().is_punct()).map(|p|p.to_string()).into_iter().collect(),
            expressions: node.syntax().children().filter_map(Expr::cast).map(Into::into)
                .collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRecordExpr {pub(crate) ast_node: RSNode, path: Option<RSPath>, fields: Vec<RSRecordExprField>, spread: Vec<RSExpr>}
impl From<RecordExpr> for RSRecordExpr {
    fn from(node: RecordExpr ) -> Self {
        RSRecordExpr{
            ast_node: node.syntax().into(),
            path: node.path().map(Into::into),
            fields: node.record_expr_field_list().map(|fl|fl.fields().map(Into::into).collect()).unwrap_or_default(),
            spread: node.record_expr_field_list().map(|fl|fl.spread().map(Into::into)).unwrap_or_default().into_iter().collect(),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRecordExprField {pub(crate) ast_node: RSNode, name: Option<RSNameRef>, expr: Vec<RSExpr>}
impl From<RecordExprField> for RSRecordExprField {
    fn from(node: RecordExprField ) -> Self {
        RSRecordExprField{
            ast_node: node.syntax().into(),
            name: node.name_ref().map(Into::into),
            expr: node.expr().map(Into::into).into_iter().collect()
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRefExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>, mutable: bool, is_ref: bool, is_const: bool}
impl From<RefExpr> for RSRefExpr {
    fn from(node:  RefExpr) -> Self {
        RSRefExpr{
            ast_node: node.syntax().into(),
            mutable: node.mut_token().is_some(),
            is_ref: node.amp_token().is_some(),
            is_const: node.const_token().is_some(),
            expr: node.expr().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSReturnExpr {pub(crate) ast_node: RSNode, pub expr: Vec<RSExpr>}
impl From<ReturnExpr> for RSReturnExpr {
    fn from(node: ReturnExpr ) -> Self {
        RSReturnExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTryExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>}
impl From<TryExpr> for RSTryExpr {
    fn from(node: TryExpr ) -> Self {
        RSTryExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTupleExpr {pub(crate) ast_node: RSNode, exprs: Vec<RSExpr>}
impl From<TupleExpr> for RSTupleExpr {
    fn from(node:  TupleExpr) -> Self {
        RSTupleExpr{
            ast_node: node.syntax().into(),
            exprs: node.fields().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSUnderscoreExpr {pub(crate) ast_node: RSNode}
impl From<UnderscoreExpr> for RSUnderscoreExpr {
    fn from(node:  UnderscoreExpr) -> Self {RSUnderscoreExpr{ast_node: node.syntax().into()}}
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSWhileExpr {pub(crate) ast_node: RSNode, expressions: Vec<RSExpr>}
impl From<WhileExpr> for RSWhileExpr {
    fn from(node: WhileExpr ) -> Self {
        RSWhileExpr{
            ast_node: node.syntax().into(),
            expressions: node.syntax().children().filter_map(Expr::cast).map(Into::into)
                .collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSYeetExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>}
impl From<YeetExpr> for RSYeetExpr {
    fn from(node: YeetExpr ) -> Self {
        RSYeetExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSYieldExpr {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>}
impl From<YieldExpr> for RSYieldExpr {
    fn from(node: YieldExpr ) -> Self {
        RSYieldExpr{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect()
        }
    }
}


#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSStmt {
    ExprStmt(RSExprStmt),
    Item(RSItem),
    LetStmt(RSLetStmt),
}


impl From<Stmt> for RSStmt {
    fn from(stmt: Stmt) -> Self {
        match stmt {
            Stmt::ExprStmt(node) => RSStmt::ExprStmt(node.into()),
            Stmt::Item(node) => RSStmt::Item(node.into()),
            Stmt::LetStmt(node) => RSStmt::LetStmt(node.into()),

        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSExprStmt {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>}
impl From<ExprStmt> for RSExprStmt {
    fn from(node: ExprStmt ) -> Self {
        RSExprStmt{ast_node: node.syntax().into(), expr: node.expr().map(Into::into).into_iter().collect()}
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLetStmt {
    pub(crate) ast_node: RSNode,
    pub initializer: Option<RSExpr>,
    pub let_else: Option<RSBlockExpr>,
    pub pat: Option<RSPat>,
    pub ty: Option<RSType>
}
impl From<LetStmt> for RSLetStmt {
    fn from(node: LetStmt ) -> Self {
        RSLetStmt{
            ast_node: node.syntax().into(),
            initializer: node.initializer().map(Into::into),
            let_else: node.let_else().map(|l|l.block_expr().map(Into::into)).flatten(),
            pat: node.pat().map(Into::into),
            ty: node.ty().map(Into::into)
        }
    }
}



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
    RecordPatField(RSRecordPatField)
}

impl From<Pat> for RSPat {
    fn from(pat: Pat) -> Self {
        match pat {
            Pat::BoxPat(node) => RSPat::BoxPat(node.into()),
            Pat::ConstBlockPat(node) => RSPat::ConstBlockPat(node.into()),
            Pat::IdentPat(node) => RSPat::IdentPat(node.into()),
            Pat::LiteralPat(node) => RSPat::LiteralPat(node.into()),
            Pat::MacroPat(node) => RSPat::MacroPat(node.into()),
            Pat::OrPat(node) => RSPat::OrPat(node.into()),
            Pat::ParenPat(node) => RSPat::ParenPat(node.into()),
            Pat::PathPat(node) => RSPat::PathPat(node.into()),
            Pat::RangePat(node) => RSPat::RangePat(node.into()),
            Pat::RecordPat(node) => RSPat::RecordPat(node.into()),
            Pat::RefPat(node) => RSPat::RefPat(node.into()),
            Pat::RestPat(node) => RSPat::RestPat(node.into()),
            Pat::SlicePat(node) => RSPat::SlicePat(node.into()),
            Pat::TuplePat(node) => RSPat::TuplePat(node.into()),
            Pat::TupleStructPat(node) => RSPat::TupleStructPat(node.into()),
            Pat::WildcardPat(node) => RSPat::WildcardPat(node.into()),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSBoxPat {pub(crate) ast_node: RSNode, pat: Vec<RSPat>}
impl From<BoxPat> for RSBoxPat {
    fn from(node: BoxPat ) -> Self {
        RSBoxPat{
            ast_node: node.syntax().into(),
            pat: node.pat().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSConstBlockPat {pub(crate) ast_node: RSNode, blockExpr: Option<RSBlockExpr>}
impl From<ConstBlockPat> for RSConstBlockPat {
    fn from(node:  ConstBlockPat) -> Self {
        RSConstBlockPat{
            ast_node: node.syntax().into(),
            blockExpr: node.block_expr().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSIdentPat {pub ast_node: RSNode, pub name:Option<String>, pat: Vec<RSPat>}
impl From<IdentPat> for RSIdentPat {
    fn from(node: IdentPat ) -> Self {
        RSIdentPat{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string()),
            pat: node.pat().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLiteralPat {pub(crate) ast_node: RSNode, literal: Option<RSLiteral>}
impl From<LiteralPat> for RSLiteralPat {
    fn from(node: LiteralPat ) -> Self {
        RSLiteralPat{
            ast_node: node.syntax().into(),
            literal: node.literal().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroPat {pub(crate) ast_node: RSNode, macro_call: Option<RSMacroCall>}
impl From<MacroPat> for RSMacroPat {
    fn from(node: MacroPat ) -> Self {
        RSMacroPat{
            ast_node: node.syntax().into(),
            macro_call: node.macro_call().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSOrPat {pub(crate) ast_node: RSNode, pats: Vec<RSPat>}
impl From<OrPat> for RSOrPat {
    fn from(node: OrPat ) -> Self {
        RSOrPat{
            ast_node: node.syntax().into(),
            pats : node.pats().map(Into::into).into_iter().collect::<Vec<_>>()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSParenPat {pub(crate) ast_node: RSNode, pat: Vec<RSPat>}
impl From<ParenPat> for RSParenPat {
    fn from(node: ParenPat ) -> Self {
        RSParenPat{
            ast_node: node.syntax().into(),
            pat: node.pat().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPathPat {pub(crate) ast_node: RSNode, path: Option<RSPath>}
impl From<PathPat> for RSPathPat {
    fn from(node: PathPat ) -> Self {
        RSPathPat{
            ast_node: node.syntax().into(),
            path: node.path().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRangePat {pub(crate) ast_node: RSNode, patterns: Vec<RSPat>, operator: String}
impl From<RangePat> for RSRangePat {
    fn from(node: RangePat ) -> Self {
        RSRangePat{
            ast_node: node.syntax().into(),
            operator: node.syntax().children_with_tokens().filter(|sn|sn.kind().is_punct()).map(|p|p.to_string()).into_iter().collect(),
            patterns: node.syntax().children().filter_map(Pat::cast).map(Into::into)
                .collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRecordPat {pub(crate) ast_node: RSNode, path: Option<RSPath>, fields: Vec<RSRecordPatField>}
impl From<RecordPat> for RSRecordPat {
    fn from(node: RecordPat ) -> Self {
        RSRecordPat{
            ast_node: node.syntax().into(),
            path: node.path().map(Into::into),
            fields: node.record_pat_field_list().map(|fl|fl.fields().map(Into::into).collect()).unwrap_or_default(),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRecordPatField {pub(crate) ast_node: RSNode, name: Option<RSNameRef>, pat: Vec<RSPat>}
impl From<RecordPatField> for RSRecordPatField {
    fn from(node: RecordPatField ) -> Self {
        RSRecordPatField{
            ast_node: node.syntax().into(),
            name: node.name_ref().map(Into::into),
            pat: node.pat().map(Into::into).into_iter().collect()
        }
    }
}



#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRefPat {pub(crate) ast_node: RSNode, pat: Vec<RSPat>, mutable: bool, is_ref: bool}
impl From<RefPat> for RSRefPat {
    fn from(node: RefPat ) -> Self {
        RSRefPat{
            ast_node: node.syntax().into(),
            pat: node.pat().map(Into::into).into_iter().collect(),
            mutable: node.mut_token().is_some(),
            is_ref: node.amp_token().is_some()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRestPat {pub(crate) ast_node: RSNode}
impl From<RestPat> for RSRestPat {
    fn from(node: RestPat ) -> Self {RSRestPat{ast_node: node.syntax().into()}}
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSSlicePat {pub(crate) ast_node: RSNode, pats: Vec<RSPat>}
impl From<SlicePat> for RSSlicePat {
    fn from(node: SlicePat ) -> Self {
        RSSlicePat{
            ast_node: node.syntax().into(),
            pats: node.pats().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTuplePat {pub(crate) ast_node: RSNode, fields: Vec<RSPat>}
impl From<TuplePat> for RSTuplePat {
    fn from(node: TuplePat ) -> Self {
        RSTuplePat{
            ast_node: node.syntax().into(),
            fields: node.fields().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTupleStructPat {pub(crate) ast_node: RSNode, path: Option<RSPath>, fields: Vec<RSPat>}
impl From<TupleStructPat> for RSTupleStructPat {
    fn from(node: TupleStructPat ) -> Self {
        RSTupleStructPat{
            ast_node: node.syntax().into(),
            path: node.path().map(Into::into),
            fields: node.fields().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSWildcardPat {pub(crate) ast_node: RSNode}
impl From<WildcardPat> for RSWildcardPat {
    fn from(node: WildcardPat ) -> Self {RSWildcardPat{ast_node: node.syntax().into()}}
}


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
        match t {
            Type::ArrayType(node) => RSType::ArrayType(node.into()),
            Type::DynTraitType(node) => RSType::DynTraitType(node.into()),
            Type::FnPtrType(node) => RSType::FnPtrType(node.into()),
            Type::ForType(node) => RSType::ForType(node.into()),
            Type::ImplTraitType(node) => RSType::ImplTraitType(node.into()),
            Type::InferType(node) => RSType::InferType(node.into()),
            Type::MacroType(node) => RSType::MacroType(node.into()),
            Type::NeverType(node) => RSType::NeverType(node.into()),
            Type::ParenType(node) => RSType::ParenType(node.into()),
            Type::PathType(node) => RSType::PathType(node.into()),
            Type::PtrType(node) => RSType::PtrType(node.into()),
            Type::RefType(node) => RSType::RefType(node.into()),
            Type::SliceType(node) => RSType::SliceType(node.into()),
            Type::TupleType(node) => RSType::TupleType(node.into()),

        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSArrayType {pub(crate) ast_node: RSNode, ty: Vec<RSType>, const_arg: Option<RSConstArg>}
impl From<ArrayType> for RSArrayType {
    fn from(node: ArrayType ) -> Self {

        RSArrayType{
            ast_node: node.syntax().into(),
            ty: node.ty().map(Into::into).into_iter().collect(),
            const_arg: node.const_arg().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSDynTraitType {pub(crate) ast_node: RSNode, type_bound_list: Vec<RSTypeBound>}
impl From<DynTraitType> for RSDynTraitType {
    fn from(node: DynTraitType ) -> Self {
        RSDynTraitType{
            ast_node: node.syntax().into(),
            type_bound_list: node.type_bound_list().iter().flat_map(|tb|tb.bounds().map(Into::into)).collect::<Vec<_>>()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSFnPtrType {pub(crate) ast_node: RSNode, param_list: Vec<RSParamList>, ret_type: Vec<RSType>}
impl From<FnPtrType> for RSFnPtrType {
    fn from(node: FnPtrType ) -> Self {
        RSFnPtrType{
            ast_node: node.syntax().into(),
            param_list: node.param_list().map(Into::into).into_iter().collect::<Vec<_>>(),
            ret_type: node.ret_type().and_then(|rt|rt.ty().map(Into::into)).into_iter().collect::<Vec<_>>()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSForType {pub(crate) ast_node: RSNode, ty: Vec<RSType>, generics_in_for: Vec<RSGenericParam>}
impl From<ForType> for RSForType {
    fn from(node: ForType ) -> Self {
        RSForType{
            ast_node: node.syntax().into(),
            ty: node.ty().map(Into::into).into_iter().collect(),
            generics_in_for: node
                .for_binder()
                .into_iter()
                .flat_map(|fb| fb.generic_param_list().into_iter())
                .flat_map(|gpl| gpl.generic_params())
                .map(Into::into)
                .collect::<Vec<_>>(),
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSImplTraitType {pub(crate) ast_node: RSNode,  type_bound_list: Vec<RSTypeBound>}
impl From<ImplTraitType> for RSImplTraitType {
    fn from(node: ImplTraitType ) -> Self {
        RSImplTraitType{
            ast_node: node.syntax().into(),
            type_bound_list: node.type_bound_list().iter().flat_map(|tb|tb.bounds().map(Into::into)).collect::<Vec<_>>(),
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSInferType {pub(crate) ast_node: RSNode}
impl From<InferType> for RSInferType {
    fn from(node: InferType ) -> Self {
        RSInferType{ast_node: node.syntax().into()}
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSMacroType {pub(crate) ast_node: RSNode, macro_call: Option<RSMacroCall>}
impl From<MacroType> for RSMacroType {
    fn from(node: MacroType ) -> Self {
        RSMacroType{
            ast_node: node.syntax().into(),
            macro_call: node.macro_call().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSNeverType {pub(crate) ast_node: RSNode}
impl From<NeverType> for RSNeverType {
    fn from(node: NeverType ) -> Self {RSNeverType{ast_node: node.syntax().into()}}
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSParenType {pub(crate) ast_node: RSNode, ty: Vec<RSType>}
impl From<ParenType> for RSParenType {
    fn from(node: ParenType ) -> Self {
        RSParenType{
            ast_node: node.syntax().into(),
            ty: node.ty().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPathType {pub(crate) ast_node: RSNode, path: Option<RSPath>}
impl From<PathType> for RSPathType {
    fn from(node: PathType ) -> Self {
        RSPathType{
            ast_node: node.syntax().into(), path: node.path().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPath {pub(crate) ast_node: RSNode, segment: Option<RSPathSegment>, qualifier: Vec<RSPath>}
impl From<Path> for RSPath {
    fn from(node: Path ) -> Self {
        RSPath{
            ast_node: node.syntax().into(),
            qualifier: node.qualifier().map(Into::into).into_iter().collect(),
            segment: node.segment().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSPtrType {pub(crate) ast_node: RSNode, ty: Vec<RSType>, has_star: bool, is_const: bool, is_mut: bool}
impl From<PtrType> for RSPtrType {
    fn from(node: PtrType ) -> Self {
        RSPtrType{
            ast_node: node.syntax().into(),
            ty: node.ty().map(Into::into).into_iter().collect(),
            has_star: node.star_token().is_some(),
            is_const: node.const_token().is_some(),
            is_mut: node.mut_token().is_some()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRefType {pub(crate) ast_node: RSNode, lifetime: Option<RSLifetime>, ty: Vec<RSType>, has_amp: bool, has_mut: bool}
impl From<RefType> for RSRefType {
    fn from(node: RefType ) -> Self {
        RSRefType{
            ast_node: node.syntax().into(),
            lifetime: node.lifetime().map(Into::into),
            ty: node.ty().map(Into::into).into_iter().collect(),
            has_amp: node.amp_token().is_some(),
            has_mut: node.mut_token().is_some()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSSliceType {pub(crate) ast_node: RSNode, ty: Vec<RSType>}
impl From<SliceType> for RSSliceType {
    fn from(node: SliceType ) -> Self {
        RSSliceType{
            ast_node: node.syntax().into(),
            ty: node.ty().map(Into::into).into_iter().collect(),
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTupleType {pub(crate) ast_node: RSNode, fields: Vec<RSType>}
impl From<TupleType> for RSTupleType {
    fn from(node: TupleType ) -> Self {
        RSTupleType{
            ast_node: node.syntax().into(),
            fields: node.fields().into_iter().map(Into::into).collect()
        }
    }
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSUseBoundGenericArg {
    Lifetime(RSLifetime),
    NameRef(RSNameRef),
}

impl From<UseBoundGenericArg> for RSUseBoundGenericArg {
    fn from(ubg: UseBoundGenericArg) -> Self {
        match ubg {
            UseBoundGenericArg::Lifetime(node) => RSUseBoundGenericArg::Lifetime(node.into()),
            UseBoundGenericArg::NameRef(node) => RSUseBoundGenericArg::NameRef(node.into()),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLifetime {pub(crate) ast_node: RSNode, name: String}
impl From<Lifetime> for RSLifetime {
    fn from(node: Lifetime ) -> Self {
        RSLifetime{
            ast_node: node.syntax().into(),
            name: node.to_string()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSNameRef {pub(crate) ast_node: RSNode, text: String, ident: Option<String>, int_number_token: Option<String>, has_cap_Self: bool, has_crate: bool, has_self: bool, has_super:bool}
impl From<NameRef> for RSNameRef {
    fn from(node: NameRef ) -> Self {
        RSNameRef{
            ast_node: node.syntax().into(),
            text: node.text().to_string(),
            ident: node.ident_token().map(|t|t.text().to_string()),
            int_number_token: node.int_number_token().map(|t|t.text().to_string()),
            has_cap_Self: node.Self_token().is_some(),
            has_crate: node.crate_token().is_some(),
            has_self: node.self_token().is_some(),
            has_super: node.super_token().is_some()
        }
    }
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSVariantDef {
    Struct(RSStruct),
    Union(RSUnion),
    Variant(RSVariant),
}

impl From<VariantDef> for RSVariantDef {
    fn from(variant: VariantDef) -> Self {
        match variant {
            VariantDef::Struct(node) => RSVariantDef::Struct(node.into()),
            VariantDef::Union(node) => RSVariantDef::Union(node.into()),
            VariantDef::Variant(node) => RSVariantDef::Variant(node.into()),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSVariant {
    pub(crate) ast_node: RSNode,
    name: Option<String>,
    expr: Vec<RSExpr>,
    fields: Vec<RSFieldList>
}
impl From<Variant> for RSVariant {
    fn from(node: Variant ) -> Self {
        RSVariant{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string()),
            expr: node.expr().map(Into::into).into_iter().collect(),
            fields: node.field_list().map(Into::into).into_iter().collect()
        }
    }
}


#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTypeBound {pub(crate) ast_node: RSNode, ty: Option<RSType>, generics_in_for: Vec<RSGenericParam>, lifetime: Option<RSLifetime>, bound_generic_args: Vec<RSUseBoundGenericArg>} //use_bound_generic_arg: UseBoundGenericArg
impl From<TypeBound> for RSTypeBound {
    fn from(node: TypeBound) -> Self {
        RSTypeBound{
            ast_node: node.syntax().into(),
            ty: node.ty().map(Into::into),
            lifetime: node.lifetime().map(Into::into),
            generics_in_for: node
                .for_binder()
                .into_iter()
                .flat_map(|fb| fb.generic_param_list().into_iter())
                .flat_map(|gpl| gpl.generic_params())
                .map(Into::into)
                .collect::<Vec<_>>(),
            bound_generic_args: node.use_bound_generic_args().into_iter().flat_map(|args|args.use_bound_generic_args().into_iter()).map(Into::into).collect::<Vec<_>>()
        }
    }
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSFieldList {
    RecordFieldList(RSRecordFieldList),
    TupleFieldList(RSTupleFieldList),
}

impl From<FieldList> for RSFieldList {
    fn from(field_list: FieldList) -> Self {
        match field_list {
            FieldList::RecordFieldList(node) => RSFieldList::RecordFieldList(node.into()),
            FieldList::TupleFieldList(node) => RSFieldList::TupleFieldList(node.into()),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRecordFieldList {pub(crate) ast_node: RSNode, fields: Vec<RSRecordField>}
impl From<RecordFieldList> for RSRecordFieldList {
    fn from(node: RecordFieldList ) -> Self {
        RSRecordFieldList{
            ast_node: node.syntax().into(),
            fields: node.fields().map(|f| f.into()).collect(),
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTupleFieldList {pub(crate) ast_node: RSNode, fields: Vec<RSTupleField>}
impl From<TupleFieldList> for RSTupleFieldList {
    fn from(node: TupleFieldList ) -> Self {
        RSTupleFieldList{
            ast_node: node.syntax().into(),
            fields: node.fields().map(|f| f.into()).collect(),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTupleField {pub(crate) ast_node: RSNode, field_type: Option<RSType>}
impl From<TupleField> for RSTupleField{
    fn from(node: TupleField ) -> Self {RSTupleField{ast_node: node.syntax().into(), field_type: node.ty().map(Into::into)}}
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSRecordField {
    pub(crate) ast_node: RSNode,
    field_type: Option<RSType>,
    expr: Option<RSExpr>,
    name: Option<String>
}
impl From<RecordField> for RSRecordField{
    fn from(node: RecordField ) -> Self {
        RSRecordField{
            ast_node: node.syntax().into(),
            field_type: node.ty().map(Into::into),
            expr: node.expr().map(Into::into),
            name: node.name().map(|n|n.to_string())
        }
    }
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSGenericArg {
    AssocTypeArg(RSAssocTypeArg),
    ConstArg(RSConstArg),
    LifetimeArg(RSLifetimeArg),
    TypeArg(RSTypeArg),
}

impl From<GenericArg> for RSGenericArg {
    fn from(generic_arg: GenericArg) -> Self {
        match generic_arg {
            GenericArg::AssocTypeArg(node) => RSGenericArg::AssocTypeArg(node.into()),
            GenericArg::ConstArg(node) => RSGenericArg::ConstArg(node.into()),
            GenericArg::LifetimeArg(node) => RSGenericArg::LifetimeArg(node.into()),
            GenericArg::TypeArg(node) => RSGenericArg::TypeArg(node.into()),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAssocTypeArg {
    pub(crate) ast_node: RSNode,
    const_arg: Option<RSConstArg>,
    name: Option<RSNameRef>,
    param_list: Option<RSParamList>,
    ret_type: Option<RSType>,
    return_type_syntax: Option<RSReturnTypeSyntax>,
    ty: Option<RSType>,
    type_bounds: Vec<RSTypeBound>,
    generic_args: Vec<RSGenericArg>,
}
impl From<AssocTypeArg> for RSAssocTypeArg {
    fn from(node: AssocTypeArg ) -> Self {
        RSAssocTypeArg{
            ast_node: node.syntax().into(),
            const_arg: node.const_arg().map(Into::into),
            name: node.name_ref().map(Into::into),
            param_list: node.param_list().map(Into::into),
            ret_type: node.ret_type().and_then(|rt|rt.ty().map(Into::into)),
            return_type_syntax: node.return_type_syntax().map(Into::into),
            ty: node.ty().map(Into::into),
            type_bounds: node.type_bound_list().iter().flat_map(|tb|tb.bounds().map(Into::into)).collect::<Vec<_>>(),
            generic_args: node.generic_arg_list().iter().flat_map(|gal|gal.generic_args().map(Into::into)).collect::<Vec<_>>()
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSConstArg {pub(crate) ast_node: RSNode, expr: Option<RSExpr>}
impl From<ConstArg> for RSConstArg {
    fn from(node: ConstArg ) -> Self {
        RSConstArg{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLifetimeArg {pub(crate) ast_node: RSNode, lifetime: Option<RSLifetime>}
impl From<LifetimeArg> for RSLifetimeArg {
    fn from(node: LifetimeArg ) -> Self {
        RSLifetimeArg{
            ast_node: node.syntax().into(),
            lifetime: node.lifetime().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTypeArg {pub(crate) ast_node: RSNode, ty: Option<RSType>}
impl From<TypeArg> for RSTypeArg {
    fn from(node: TypeArg ) -> Self {
        RSTypeArg{
            ast_node: node.syntax().into(),
            ty: node.ty().map(Into::into)
        }
    }
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSGenericParam {
    ConstParam(RSConstParam),
    LifetimeParam(RSLifetimeParam),
    TypeParam(RSTypeParam),
}

impl From<GenericParam> for RSGenericParam {
    fn from(generic_param: GenericParam) -> Self {
        match generic_param {
            GenericParam::ConstParam(node) => RSGenericParam::ConstParam(node.into()),
            GenericParam::LifetimeParam(node) => RSGenericParam::LifetimeParam(node.into()),
            GenericParam::TypeParam(node) => RSGenericParam::TypeParam(node.into()),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSConstParam {pub(crate) ast_node: RSNode, default_val: Option<RSConstArg>, ty: Option<RSType>}
impl From<ConstParam> for RSConstParam {
    fn from(node: ConstParam ) -> Self {
        RSConstParam{
            ast_node: node.syntax().into(),
            default_val: node.default_val().map(Into::into),
            ty: node.ty().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSLifetimeParam {pub(crate) ast_node: RSNode, lifetime: Option<RSLifetime>, type_bound_list: Vec<RSTypeBound>}
impl From<LifetimeParam> for RSLifetimeParam {
    fn from(node: LifetimeParam ) -> Self {
        RSLifetimeParam{
            ast_node: node.syntax().into(),
            lifetime: node.lifetime().map(Into::into),
            type_bound_list: node.type_bound_list().iter().flat_map(|tb|tb.bounds().map(Into::into)).collect::<Vec<_>>(),
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSTypeParam {pub(crate) ast_node: RSNode, name: Option<String>, type_bound_list: Vec<RSTypeBound>, default_type: Option<RSType>}
impl From<TypeParam> for RSTypeParam {
    fn from(node: TypeParam ) -> Self {
        RSTypeParam{
            ast_node: node.syntax().into(),
            name: node.name().map(|n|n.to_string()),
            type_bound_list: node.type_bound_list().iter().flat_map(|tb|tb.bounds().map(Into::into)).collect::<Vec<_>>(),
            default_type: node.default_type().map(Into::into)

        }
    }
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSExternItem {
    Fn(RSFn),
    MacroCall(RSMacroCall),
    Static(RSStatic),
    TypeAlias(RSTypeAlias),
}

impl From<ExternItem> for RSExternItem {
    fn from(ext_item: ExternItem) -> Self {
        match ext_item {
            ExternItem::Fn(node) => RSExternItem::Fn(node.into()),
            ExternItem::MacroCall(node) => RSExternItem::MacroCall(node.into()),
            ExternItem::Static(node) => RSExternItem::Static(node.into()),
            ExternItem::TypeAlias(node) => RSExternItem::TypeAlias(node.into())
        }
    }
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSAsmOperand {
    AsmConst(RSAsmConst),
    AsmLabel(RSAsmLabel),
    AsmRegOperand(RSAsmRegOperand),
    AsmSym(RSAsmSym),
}

impl From<AsmOperand> for RSAsmOperand {
    fn from(asm_op: AsmOperand) -> Self {
        match asm_op {
            AsmOperand::AsmConst(node) => RSAsmOperand::AsmConst(node.into()),
            AsmOperand::AsmLabel(node) => RSAsmOperand::AsmLabel(node.into()),
            AsmOperand::AsmRegOperand(node) => RSAsmOperand::AsmRegOperand(node.into()),
            AsmOperand::AsmSym(node) => RSAsmOperand::AsmSym(node.into()),
        }
    }
}


#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmConst {pub(crate) ast_node: RSNode, expr: Vec<RSExpr>}
impl From<AsmConst> for RSAsmConst {
    fn from(node: AsmConst ) -> Self {
        RSAsmConst{
            ast_node: node.syntax().into(),
            expr: node.expr().map(Into::into).into_iter().collect()
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmLabel {pub(crate) ast_node: RSNode, block_expr: Option<RSBlockExpr>}
impl From<AsmLabel> for RSAsmLabel {
    fn from(node: AsmLabel) -> Self {
        RSAsmLabel{
            ast_node: node.syntax().into(),
            block_expr: node.block_expr().map(Into::into)
        }
    }
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmRegOperand {pub(crate) ast_node: RSNode}
impl From<AsmRegOperand> for RSAsmRegOperand {
    fn from(node: AsmRegOperand ) -> Self {RSAsmRegOperand{ast_node: node.syntax().into()}}
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmSym {pub(crate) ast_node: RSNode}
impl From<AsmSym> for RSAsmSym {
    fn from(node: AsmSym ) -> Self {RSAsmSym{ast_node: node.syntax().into()}}
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSAsmPiece {
    AsmClobberAbi(RSAsmClobberAbi),
    AsmOperandNamed(RSAsmOperandNamed),
    AsmOptions(RSAsmOptions),
}

impl From<AsmPiece> for RSAsmPiece {
    fn from(asm_p: AsmPiece) -> Self {
        match asm_p {
            AsmPiece::AsmClobberAbi(node) => RSAsmPiece::AsmClobberAbi(node.into()),
            AsmPiece::AsmOperandNamed(node) => RSAsmPiece::AsmOperandNamed(node.into()),
            AsmPiece::AsmOptions(node) => RSAsmPiece::AsmOptions(node.into()),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmClobberAbi {pub(crate) ast_node: RSNode}
impl From<AsmClobberAbi> for RSAsmClobberAbi {
    fn from(node: AsmClobberAbi ) -> Self {RSAsmClobberAbi{ast_node: node.syntax().into()}}
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmOperandNamed {pub(crate) ast_node: RSNode}
impl From<AsmOperandNamed> for RSAsmOperandNamed {
    fn from(node: AsmOperandNamed ) -> Self {RSAsmOperandNamed{ast_node: node.syntax().into()}}
}
#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSAsmOptions {pub(crate) ast_node: RSNode}
impl From<AsmOptions> for RSAsmOptions {
    fn from(node: AsmOptions ) -> Self {RSAsmOptions{ast_node: node.syntax().into()}}
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSAssocItem {
    Const(RSConst),
    Fn(RSFn),
    MacroCall(RSMacroCall),
    TypeAlias(RSTypeAlias),
}

impl From<AssocItem> for RSAssocItem {
    fn from(assoc_item: AssocItem) -> Self {
        match assoc_item {
            AssocItem::Const(node) => RSAssocItem::Const(node.into()),
            AssocItem::Fn(node) => RSAssocItem::Fn(node.into()),
            AssocItem::MacroCall(node) => RSAssocItem::MacroCall(node.into()),
            AssocItem::TypeAlias(node) => RSAssocItem::TypeAlias(node.into()),
        }
    }
}

#[derive(uniffi::Enum)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RSAdt {
    Enum(RSEnum),
    Struct(RSStruct),
    Union(RSUnion),
}

impl From<Adt> for RSAdt {
    fn from(adt: Adt) -> Self {
        match adt {
            Adt::Enum(node) => RSAdt::Enum(node.into()),
            Adt::Struct(node) => RSAdt::Struct(node.into()),
            Adt::Union(node) => RSAdt::Union(node.into()),
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSParam {
    pub(crate) ast_node: RSNode,
    pub pat: Option<RSPat>,
    pub ty: Option<RSType>,
}
impl From<Param> for RSParam {
    fn from(node: Param) -> Self {
        RSParam{
            ast_node: node.syntax().into(),
            pat: node.pat().map(Into::into),
            ty: node.ty().map(Into::into)
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSParamList {
    pub(crate) ast_node: RSNode,
    pub params: Vec<RSParam>,
    pub self_param: Option<RSSelfParam>
}
impl From<ParamList> for RSParamList {
    fn from(node: ParamList ) -> Self {
        RSParamList{
            ast_node: node.syntax().into(),
            params: node.params().map(Into::into).collect(),
            self_param: node.self_param().map(Into::into)
        }
    }
}

#[derive(uniffi::Record)]
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RSSelfParam {
    pub(crate) ast_node: RSNode,
    ty: Option<RSType>,
    lifetime: Option<RSLifetime>
}
impl From<SelfParam> for RSSelfParam {
    fn from(node: SelfParam) -> Self {
        RSSelfParam{
            ast_node: node.syntax().into(),
            lifetime: node.lifetime().map(Into::into),
            ty: node.ty().map(Into::into)
        }
    }
}

