/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.graph.ast.declarations

import de.fraunhofer.aisec.cpg.PopulatedByPass
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.scopes.FileScope
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.scopes.SymbolMap
import de.fraunhofer.aisec.cpg.helpers.neo4j.SimpleNameConverter
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import org.neo4j.ogm.annotation.typeconversion.Convert

/**
 * This class represents a real *import* of one or more symbols of a specified [NameScope] (e.g.,
 * defined by a [NamespaceDeclaration]) into the current scope. Depending on the language, this can
 * be only used at the global or package scope (e.g. in Go and Java with the `import` keyword) or in
 * any scope (e.g. in C++ with the `using` keyword).
 *
 * ### Examples (Go)
 *
 * In Go, we usually import the package itself as a symbol (see [ImportStyle.IMPORT_NAMESPACE]).
 *
 * ```Go
 * package p
 *
 * import(
 *   // standard library imports have only relative import
 *   // paths
 *   "os"
 *
 *   // packages from external sources have a hostname and
 *   // path
 *   "golang.org/x/oauth2"
 *
 *   // we can also define an alias, to avoid conflicts
 *   // (in this case with the config variable)
 *   configpkg "example.com/library/config"
 * )
 *
 * func main() {
 *   // We can then use symbols of the particular
 *   // package by prefixing the symbol with the
 *   // imported name
 *   file, err := os.Open("myfile")
 *
 *   var config oauth2.Config
 *
 *   configpkg.DoAwesome(&config)
 * }
 * ```
 *
 * In this example we set [import] and [name] to the names `os` and `oauth2` respectively. For the
 * first import, the [importURL] is empty, for the second import [importURL] is
 * `golang.org/x/oauth2`.
 *
 * In the last import, the property [alias] is used to import the symbol using an alias. In this
 * case the [import] is still `config`, but the [name] is the same as the alias (`configpkg`), so
 * that the symbol resolver can find it.
 *
 * The import is valid for the whole file, where it is imported, i.e., its [FileScope].
 *
 * ### Examples (C++)
 *
 * In C++, we can import a single symbol using its fully qualified name.
 *
 * ```cpp
 * namespace std {
 *   class string {};
 * }
 *
 * int main() {
 *   using std::string;
 *   string s;
 *   return 1;
 * }
 * ```
 *
 * The imported symbol is then visible within the current [Scope] of the [ImportDeclaration]. In the
 * example [name] and [import] is set to `std::string`, [style] is
 * [ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE].
 *
 * Another possibility is to import a complete namespace, or to be more precise import all symbols
 * of the specified namespace into the current scope.
 *
 * ```cpp
 * namespace std {
 *   class string {};
 * }
 *
 * int main() {
 *   using namespace std;
 *   string s;
 *   return 1;
 * }
 * ```
 *
 * In this example, the [name] and [import] is set to `std` and [style] is
 * [ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE].
 */
class ImportDeclaration : Declaration() {

    /**
     * The imported symbol: This usually refers to a [NamespaceDeclaration] / its [NameScope] or a
     * [Declaration] within this namespace. This will always refer to the original name of the
     * imported symbol, even though an alias is used.
     * * If no alias is used, the [name] of this declaration is also set to the same name as the
     *   imported symbol.
     * * If an alias is used, the [name] of this declaration is set to the value of [alias].
     */
    @Convert(value = SimpleNameConverter::class) var import: Name = Name(EMPTY_NAME)

    /**
     * Some languages support the use of aliases in importing symbols, for example to avoid
     * conflicts with already named symbols.
     *
     * In case this is used, the [name] is also set to the value of [alias], so in theory this
     * property would be not needed, instead we could check whether [name] and [import] point to
     * different names. However, in practice, it is easier to have this as an extra property to
     * quickly identify imports with aliases.
     */
    @Convert(value = SimpleNameConverter::class) var alias: Name? = null

    /**
     * In some languages (such as Go), we can specify packages to fetch from external sources. In
     * this case, this property can be used to hold the import url.
     */
    var importURL: String? = null

    /** The import style. */
    var style: ImportStyle = ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE

    /**
     * A list of symbols that this declaration imports. This will be populated by
     * [ImportResolver.handleImportDeclaration].
     */
    @Transient
    @PopulatedByPass(ImportResolver::class)
    var importedSymbols: SymbolMap = mutableMapOf()
}
