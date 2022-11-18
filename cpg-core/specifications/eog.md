# Specification: Evaluation Order Graph

The Evaluation Order Graph (EOG) is built as edges between AST nodes after the initial language to CPG translation. Its purpose is to follow the order in which code is executed, similar to a CFG, and additionally differentiate on a finer level of granularity in which order expressions and subexpressions are evaluated. Each node that is part of the graph points to a set of previously evaluated nodes (`prevEOG`) and nodes that are evaluated after (`nextEOG`). In the following, we summarize in which order the root node representing a language construct and its descendants in the AST tree are connected.

 An EOG always starts at the header of a method/function or record that holds code and ends in one (implicit) or multiple
 return statements. A implicit return statement with a code location of (-1,-1) is used if the
 actual source code does not have an explicit return statement.
 
 A distinct EOG is drawn for any declared component that can contain code, currently: `NamespaceDeclaration`, `TranslationUnitDeclaration`, `RecordDeclaration` and any Subclass of `FunctionDeclaration`.
 
 The EOG is similar to the CFG `ControlFlowGraphPass`, but there are some subtle differences:
 * For methods without explicit return statement, EOF will have an edge to a virtual return node  with line number -1 which does not exist in the original code. A CFG will always end with the last reachable statement(s) and not insert any virtual return statements.
 * EOG considers an opening blocking ("CompoundStatement", indicated by a "{") as a separate node. A CFG will rather use the first actual executable statement within the block.
 * For IF statements, EOG treats the "if" keyword and the condition as separate nodes. CFG treats this as one "if" statement.
 * EOG considers a method header as a node. CFG will consider the first executable statement of the methods as a node.

