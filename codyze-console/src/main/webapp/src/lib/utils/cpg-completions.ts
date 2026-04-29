// AUTO-GENERATED — do not edit manually.
// Regenerate by running: pnpm run generate:completions
// Source: cpg-core/src/main/kotlin/de/fraunhofer/aisec/cpg/graph/Extensions.kt

/** Completion item shape used by MonacoEditor.svelte */
export interface CompletionEntry {
  label: string;
  detail: string;
  documentation?: string;
  /** 'function' marks items that should be offered as methods (insertText gets a '(' appended) */
  kind?: 'function';
}

/**
 * CPG Shortcut API extensions on {@link AstNode} / {@link TranslationResult}.
 * Generated from {@code val AstNode?.xxx} and {@code fun TranslationResult.xxx} declarations
 * in Extensions.kt.
 */
export const cpgResultProperties: CompletionEntry[] = [
  { label: "nodes", detail: "List<AstNode>",
    documentation: "Returns all [Node] children in the AST-subgraph, starting with this [Node]." },
  { label: "calls", detail: "List<Call>",
    documentation: "Returns all [Call] children in this graph, starting with this [Node]." },
  { label: "operatorCalls", detail: "List<OperatorCall>",
    documentation: "Returns all [OperatorCall] children in this graph, starting with this [Node]." },
  { label: "mcalls", detail: "List<MemberCall>",
    documentation: "Returns all [MemberCall] children in this graph, starting with this [Node]." },
  { label: "casts", detail: "List<Cast>",
    documentation: "Returns all [Cast] children in this graph, starting with this [Node]." },
  { label: "methods", detail: "List<Method>",
    documentation: "Returns all [Method] children in this graph, starting with this [Node]." },
  { label: "operators", detail: "List<Operator>",
    documentation: "Returns all [Operator] children in this graph, starting with this [Node]." },
  { label: "fields", detail: "List<Field>",
    documentation: "Returns all [Field] children in this graph, starting with this [Node]." },
  { label: "parameters", detail: "List<Parameter>",
    documentation: "Returns all [Parameter] children in this graph, starting with this [Node]." },
  { label: "functions", detail: "List<Function>",
    documentation: "Returns all [Function] children in this graph, starting with this [Node]." },
  { label: "records", detail: "List<Record>",
    documentation: "Returns all [Record] children in this graph, starting with this [Node]." },
  { label: "namespaces", detail: "List<Namespace>",
    documentation: "Returns all [Record] children in this graph, starting with this [Node]." },
  { label: "imports", detail: "List<Import>",
    documentation: "Returns all [Import] children in this graph, starting with this [Node]." },
  { label: "variables", detail: "List<Variable>",
    documentation: "Returns all [Variable] children in this graph, starting with this [Node]." },
  { label: "literals", detail: "List<Literal<*>>",
    documentation: "Returns all [Literal] children in this graph, starting with this [Node]." },
  { label: "blocks", detail: "List<Block>",
    documentation: "Returns all [Block] child edges in this graph, starting with this [Node]." },
  { label: "refs", detail: "List<Reference>",
    documentation: "Returns all [Reference] children in this graph, starting with this [Node]." },
  { label: "memberExpressions", detail: "List<MemberAccess>",
    documentation: "Returns all [MemberAccess] children in this graph, starting with this [Node]." },
  { label: "statements", detail: "List<Expression>",
    documentation: "Returns all [Expression] child edges in this graph, starting with this [Node]." },
  { label: "forLoops", detail: "List<For>",
    documentation: "Returns all [For] child edges in this graph, starting with this [Node]." },
  { label: "trys", detail: "List<Try>",
    documentation: "Returns all [Try] child edges in this graph, starting with this [Node]." },
  { label: "throws", detail: "List<Throw>",
    documentation: "Returns all [Throw] child edges in this graph, starting with this [Node]." },
  { label: "forEachLoops", detail: "List<ForEach>",
    documentation: "Returns all [ForEach] child edges in this graph, starting with this [Node]." },
  { label: "switches", detail: "List<Switch>",
    documentation: "Returns all [Switch] child edges in this graph, starting with this [Node]." },
  { label: "whileLoops", detail: "List<While>",
    documentation: "Returns all [While] child edges in this graph, starting with this [Node]." },
  { label: "doLoops", detail: "List<DoWhile>",
    documentation: "Returns all [DoWhile] child edges in this graph, starting with this [Node]." },
  { label: "breaks", detail: "List<Break>",
    documentation: "Returns all [Break] child edges in this graph, starting with this [Node]." },
  { label: "continues", detail: "List<Continue>",
    documentation: "Returns all [Continue] child edges in this graph, starting with this [Node]." },
  { label: "ifs", detail: "List<IfElse>",
    documentation: "Returns all [IfElse] child edges in this graph, starting with this [Node]." },
  { label: "labels", detail: "List<Label>",
    documentation: "Returns all [Label] child edges in this graph, starting with this [Node]." },
  { label: "returns", detail: "List<Return>",
    documentation: "Returns all [Return] child edges in this graph, starting with this [Node]." },
  { label: "assigns", detail: "List<Assign>",
    documentation: "Returns all [Assign] child edges in this graph, starting with this [Node]." },
  { label: "problems", detail: "List<ProblemNode>",
    documentation: "Return all [ProblemNode] children in this graph (either stored directly or in [Node.additionalProblems]), starting with this [Node]." },
  { label: "assignments", detail: "List<Assignment>",
    documentation: "Returns all [Assignment] child edges in this graph, starting with this [Node]." },
  { label: "callsByName", detail: "(name: String) -> List<Call> {",
    documentation: "Returns all [Call]s in this graph which call a method with the given [name].", kind: 'function' },
  { label: "callersOf", detail: "(function: Function) -> Set<Function> {",
    documentation: "Set of all functions calling [function]", kind: 'function' }
];

/**
 * Standard Kotlin collection / stdlib methods offered after any {@code .} trigger.
 * These are hand-maintained because there is no single Kotlin source file to generate them from.
 */
export const kotlinCollectionMethods: CompletionEntry[] = [
  { label: 'filter', detail: '(predicate: (T) -> Boolean) -> List<T>', documentation: 'Returns a list containing only elements matching the given predicate' },
  { label: 'filterNot', detail: '(predicate: (T) -> Boolean) -> List<T>', documentation: 'Returns a list containing only elements not matching the given predicate' },
  { label: 'map', detail: '(transform: (T) -> R) -> List<R>', documentation: 'Returns a list of results of applying the given transform to each element' },
  { label: 'mapNotNull', detail: '(transform: (T) -> R?) -> List<R>', documentation: 'Returns a list containing only non-null results of the transform' },
  { label: 'flatMap', detail: '(transform: (T) -> Iterable<R>) -> List<R>', documentation: 'Returns a single list of all elements yielded by transform' },
  { label: 'forEach', detail: '(action: (T) -> Unit) -> Unit', documentation: 'Performs the given action on each element' },
  { label: 'forEachIndexed', detail: '(action: (Int, T) -> Unit) -> Unit', documentation: 'Performs the given action on each element with its index' },
  { label: 'any', detail: '(predicate: (T) -> Boolean) -> Boolean', documentation: 'Returns true if at least one element matches the predicate' },
  { label: 'all', detail: '(predicate: (T) -> Boolean) -> Boolean', documentation: 'Returns true if all elements match the predicate' },
  { label: 'none', detail: '(predicate: (T) -> Boolean) -> Boolean', documentation: 'Returns true if no elements match the predicate' },
  { label: 'count', detail: '(predicate: (T) -> Boolean) -> Int', documentation: 'Returns the count of elements matching the predicate' },
  { label: 'find', detail: '(predicate: (T) -> Boolean) -> T?', documentation: 'Returns the first element matching the predicate, or null' },
  { label: 'firstOrNull', detail: '(predicate: (T) -> Boolean) -> T?', documentation: 'Returns the first element matching the predicate, or null' },
  { label: 'first', detail: '(predicate: (T) -> Boolean) -> T', documentation: 'Returns the first element matching the predicate' },
  { label: 'last', detail: '(predicate: (T) -> Boolean) -> T', documentation: 'Returns the last element matching the predicate' },
  { label: 'lastOrNull', detail: '(predicate: (T) -> Boolean) -> T?', documentation: 'Returns the last element matching the predicate, or null' },
  { label: 'sortedBy', detail: '(selector: (T) -> R) -> List<T>', documentation: 'Returns a list sorted by the given selector' },
  { label: 'sortedByDescending', detail: '(selector: (T) -> R) -> List<T>', documentation: 'Returns a list sorted descending by the given selector' },
  { label: 'groupBy', detail: '(keySelector: (T) -> K) -> Map<K, List<T>>', documentation: 'Groups elements by the given key selector' },
  { label: 'associate', detail: '(transform: (T) -> Pair<K, V>) -> Map<K, V>', documentation: 'Returns a Map from the given transform pairs' },
  { label: 'associateBy', detail: '(keySelector: (T) -> K) -> Map<K, T>', documentation: 'Returns a Map keyed by the given selector' },
  { label: 'distinct', detail: '() -> List<T>', documentation: 'Returns a list with only distinct elements' },
  { label: 'distinctBy', detail: '(selector: (T) -> K) -> List<T>', documentation: 'Returns a list with elements having distinct keys' },
  { label: 'take', detail: '(n: Int) -> List<T>', documentation: 'Returns a list of the first n elements' },
  { label: 'drop', detail: '(n: Int) -> List<T>', documentation: 'Returns a list skipping the first n elements' },
  { label: 'chunked', detail: '(size: Int) -> List<List<T>>', documentation: 'Splits the collection into chunks of the given size' },
  { label: 'joinToString', detail: '(separator: String) -> String', documentation: 'Creates a string from all elements separated by the given separator' },
  { label: 'toList', detail: '() -> List<T>', documentation: 'Converts to a List' },
  { label: 'toSet', detail: '() -> Set<T>', documentation: 'Converts to a Set' },
  { label: 'toMutableList', detail: '() -> MutableList<T>', documentation: 'Converts to a MutableList' },
  { label: 'sumOf', detail: '(selector: (T) -> Int) -> Int', documentation: 'Returns the sum of all values produced by the selector' },
  { label: 'maxOf', detail: '(selector: (T) -> R) -> R', documentation: 'Returns the maximum value produced by the selector' },
  { label: 'minOf', detail: '(selector: (T) -> R) -> R', documentation: 'Returns the minimum value produced by the selector' },
  { label: 'flatten', detail: '() -> List<T>', documentation: 'Returns a single list of all elements in nested collections' },
  { label: 'zip', detail: '(other: Iterable<R>) -> List<Pair<T, R>>', documentation: 'Returns a list of pairs built from elements at the same positions' },
  { label: 'windowed', detail: '(size: Int) -> List<List<T>>', documentation: 'Returns a list of snapshots of the window of given size' },
  { label: 'plus', detail: '(elements: Collection<T>) -> List<T>', documentation: 'Returns a list containing all elements of this and the given collection' },
  { label: 'minus', detail: '(elements: Collection<T>) -> List<T>', documentation: 'Returns a list containing all elements except those in the given collection' },
  { label: 'intersect', detail: '(other: Iterable<T>) -> Set<T>', documentation: 'Returns a set containing elements present in both collections' },
  { label: 'union', detail: '(other: Iterable<T>) -> Set<T>', documentation: 'Returns a set of all distinct elements from both collections' },
  { label: 'size', detail: 'Int', documentation: 'The number of elements in this collection' },
  { label: 'isEmpty', detail: '() -> Boolean', documentation: 'Returns true if the collection is empty' },
  { label: 'isNotEmpty', detail: '() -> Boolean', documentation: 'Returns true if the collection is not empty' }
];

/**
 * Common CPG node properties offered after any {@code .} trigger.
 * Hand-maintained based on {@link Node}, {@link AstNode}, and {@link Expression}.
 */
export const cpgNodeProperties: CompletionEntry[] = [
  { label: 'name', detail: 'Name', documentation: 'The qualified name of this node' },
  { label: 'localName', detail: 'String', documentation: 'The local (simple) name' },
  { label: 'location', detail: 'PhysicalLocation?', documentation: 'The physical source location' },
  { label: 'astParent', detail: 'AstNode?', documentation: 'The parent node in the AST' },
  { label: 'astChildren', detail: 'List<AstNode>', documentation: 'The direct children in the AST' },
  { label: 'nextDFG', detail: 'Set<Node>', documentation: 'Next data flow graph edges' },
  { label: 'prevDFG', detail: 'Set<Node>', documentation: 'Previous data flow graph edges' },
  { label: 'nextEOG', detail: 'List<Node>', documentation: 'Next evaluation order graph edges' },
  { label: 'prevEOG', detail: 'List<Node>', documentation: 'Previous evaluation order graph edges' },
  { label: 'type', detail: 'Type', documentation: 'The inferred type of this node' },
  { label: 'language', detail: 'Language<*>?', documentation: 'The language this node belongs to' },
  { label: 'code', detail: 'String?', documentation: 'The original source code of this node' },
  { label: 'comment', detail: 'String?', documentation: 'A comment attached to this node' },
  { label: 'annotations', detail: 'List<Annotation>', documentation: 'Annotations on this node' }
];
