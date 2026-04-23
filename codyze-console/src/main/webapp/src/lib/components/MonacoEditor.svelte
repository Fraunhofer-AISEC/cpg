<script lang="ts">
  import { onMount, onDestroy } from 'svelte';

  interface Props {
    value: string;
    language?: string;
    height?: string;
    onchange?: (value: string) => void;
  }

  let { value = $bindable(''), language = 'kotlin', height = '320px', onchange }: Props = $props();

  let containerEl: HTMLDivElement;
  let editorInstance: import('monaco-editor').editor.IStandaloneCodeEditor | null = null;
  let monacoModule: typeof import('monaco-editor') | null = null;
  let isUpdatingFromParent = false;

  // Kotlin Monarch tokenizer definition
  const kotlinLanguageConfig = {
    brackets: [
      ['{', '}'] as [string, string],
      ['[', ']'] as [string, string],
      ['(', ')'] as [string, string]
    ],
    comments: {
      lineComment: '//',
      blockComment: ['/*', '*/'] as [string, string]
    },
    autoClosingPairs: [
      { open: '{', close: '}' },
      { open: '[', close: ']' },
      { open: '(', close: ')' },
      { open: '"', close: '"' },
      { open: "'", close: "'" }
    ]
  };

  const kotlinTokensProvider = {
    defaultToken: '',
    tokenPostfix: '.kotlin',

    keywords: [
      'abstract',
      'actual',
      'annotation',
      'as',
      'break',
      'by',
      'catch',
      'class',
      'companion',
      'const',
      'constructor',
      'continue',
      'crossinline',
      'data',
      'delegate',
      'do',
      'dynamic',
      'else',
      'enum',
      'expect',
      'external',
      'field',
      'file',
      'final',
      'finally',
      'for',
      'fun',
      'get',
      'if',
      'import',
      'in',
      'infix',
      'init',
      'inline',
      'inner',
      'interface',
      'internal',
      'is',
      'it',
      'lateinit',
      'noinline',
      'null',
      'object',
      'open',
      'operator',
      'out',
      'override',
      'package',
      'param',
      'private',
      'property',
      'protected',
      'public',
      'receiver',
      'reified',
      'return',
      'sealed',
      'set',
      'setparam',
      'super',
      'suspend',
      'tailrec',
      'this',
      'throw',
      'true',
      'false',
      'try',
      'typealias',
      'typeof',
      'val',
      'var',
      'vararg',
      'when',
      'where',
      'while'
    ],

    typeKeywords: [
      'Any',
      'Boolean',
      'Byte',
      'Char',
      'Double',
      'Float',
      'Int',
      'Long',
      'Nothing',
      'Number',
      'Short',
      'String',
      'Unit',
      'Array',
      'List',
      'MutableList',
      'Map',
      'MutableMap',
      'Set',
      'MutableSet',
      'Collection',
      'Iterable',
      'Sequence',
      'Pair',
      'Triple'
    ],

    operators: [
      '=',
      '>',
      '<',
      '!',
      '~',
      '?',
      ':',
      '==',
      '<=',
      '>=',
      '!=',
      '&&',
      '||',
      '++',
      '--',
      '+',
      '-',
      '*',
      '/',
      '&',
      '|',
      '^',
      '%',
      '->',
      '=>'
    ],

    symbols: /[=><!~?:&|+\-*\/\^%]+/,

    escapes: /\\(?:[abfnrtv\\"']|x[0-9A-Fa-f]{1,4}|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8})/,

    tokenizer: {
      root: [
        // Annotations
        [/@[a-zA-Z_$][\w$]*/, 'annotation'],

        // Identifiers and keywords
        [
          /[a-zA-Z_$][\w$]*/,
          {
            cases: {
              '@keywords': 'keyword',
              '@typeKeywords': 'type',
              '@default': 'identifier'
            }
          }
        ],

        // Whitespace
        { include: '@whitespace' },

        // Delimiters and operators
        [/[{}()\[\]]/, '@brackets'],
        [/[<>](?!@symbols)/, '@brackets'],
        [
          /@symbols/,
          {
            cases: {
              '@operators': 'operator',
              '@default': ''
            }
          }
        ],

        // Numbers
        [/\d*\.\d+([eE][\-+]?\d+)?[fFdD]?/, 'number.float'],
        [/0[xX][0-9a-fA-F]+/, 'number.hex'],
        [/\d+[lL]?/, 'number'],

        // Delimiter
        [/[;,.]/, 'delimiter'],

        // Strings
        [/"""/, { token: 'string.quote', bracket: '@open', next: '@triplestring' }],
        [/"([^"\\]|\\.)*$/, 'string.invalid'],
        [/"/, { token: 'string.quote', bracket: '@open', next: '@string' }],

        // Characters
        [/'[^\\']'/, 'string'],
        [/(')(@escapes)(')/, ['string', 'string.escape', 'string']],
        [/'/, 'string.invalid']
      ],

      whitespace: [
        [/[ \t\r\n]+/, 'white'],
        [/\/\*/, 'comment', '@comment'],
        [/\/\/.*$/, 'comment']
      ],

      comment: [
        [/[^\/*]+/, 'comment'],
        [/\/\*/, 'comment', '@push'],
        [/\*\//, 'comment', '@pop'],
        [/[\/*]/, 'comment']
      ],

      string: [
        [/[^\\"$]+/, 'string'],
        [/@escapes/, 'string.escape'],
        [/\\./, 'string.escape.invalid'],
        [/\$\{/, { token: 'string.template', next: '@templateExpression' }],
        [/\$[a-zA-Z_$][\w$]*/, 'string.template'],
        [/"/, { token: 'string.quote', bracket: '@close', next: '@pop' }]
      ],

      triplestring: [
        [/[^"$]+/, 'string'],
        [/"""/, { token: 'string.quote', bracket: '@close', next: '@pop' }],
        [/"/, 'string'],
        [/\$\{/, { token: 'string.template', next: '@templateExpression' }],
        [/\$[a-zA-Z_$][\w$]*/, 'string.template']
      ],

      templateExpression: [
        [/\{/, 'string.template', '@push'],
        [/\}/, { token: 'string.template', next: '@pop' }],
        { include: 'root' }
      ]
    }
  };

  // CPG Shortcut API completion items for `result.*`
  const cpgResultProperties = [
    {
      label: 'nodes',
      detail: 'List<AstNode>',
      documentation: 'All AST nodes in the analysis result'
    },
    {
      label: 'functions',
      detail: 'List<Function>',
      documentation: 'All function declarations (top-level and methods)'
    },
    {
      label: 'calls',
      detail: 'List<Call>',
      documentation: 'All call expressions in the analysis result'
    },
    {
      label: 'mcalls',
      detail: 'List<MemberCall>',
      documentation: 'All member call expressions'
    },
    {
      label: 'operatorCalls',
      detail: 'List<OperatorCall>',
      documentation: 'All operator call expressions'
    },
    {
      label: 'variables',
      detail: 'List<Variable>',
      documentation: 'All variable declarations'
    },
    {
      label: 'records',
      detail: 'List<Record>',
      documentation: 'All record (class/struct) declarations'
    },
    {
      label: 'methods',
      detail: 'List<Method>',
      documentation: 'All method declarations'
    },
    {
      label: 'fields',
      detail: 'List<Field>',
      documentation: 'All field declarations'
    },
    {
      label: 'parameters',
      detail: 'List<Parameter>',
      documentation: 'All function/method parameters'
    },
    {
      label: 'namespaces',
      detail: 'List<Namespace>',
      documentation: 'All namespace declarations'
    },
    {
      label: 'imports',
      detail: 'List<Import>',
      documentation: 'All import statements'
    },
    {
      label: 'literals',
      detail: 'List<Literal<*>>',
      documentation: 'All literal values'
    },
    {
      label: 'refs',
      detail: 'List<Reference>',
      documentation: 'All reference expressions'
    },
    {
      label: 'memberExpressions',
      detail: 'List<MemberAccess>',
      documentation: 'All member access expressions'
    },
    {
      label: 'statements',
      detail: 'List<Expression>',
      documentation: 'All statement expressions'
    },
    {
      label: 'blocks',
      detail: 'List<Block>',
      documentation: 'All block statements'
    },
    {
      label: 'forLoops',
      detail: 'List<For>',
      documentation: 'All for loop statements'
    },
    {
      label: 'forEachLoops',
      detail: 'List<ForEach>',
      documentation: 'All for-each loop statements'
    },
    {
      label: 'whileLoops',
      detail: 'List<While>',
      documentation: 'All while loop statements'
    },
    {
      label: 'ifs',
      detail: 'List<IfElse>',
      documentation: 'All if/else statements'
    },
    {
      label: 'switches',
      detail: 'List<Switch>',
      documentation: 'All switch/when statements'
    },
    {
      label: 'returns',
      detail: 'List<Return>',
      documentation: 'All return statements'
    },
    {
      label: 'assigns',
      detail: 'List<Assign>',
      documentation: 'All assignment expressions'
    },
    {
      label: 'casts',
      detail: 'List<Cast>',
      documentation: 'All cast expressions'
    },
    {
      label: 'operators',
      detail: 'List<Operator>',
      documentation: 'All operator declarations'
    },
    {
      label: 'components',
      detail: 'List<Component>',
      documentation: 'All components in the translation result'
    }
  ];

  // Common Kotlin collection/standard library methods for completions
  const kotlinCollectionMethods = [
    {
      label: 'filter',
      detail: '(predicate: (T) -> Boolean) -> List<T>',
      documentation: 'Returns a list containing only elements matching the given predicate'
    },
    {
      label: 'filterNot',
      detail: '(predicate: (T) -> Boolean) -> List<T>',
      documentation: 'Returns a list containing only elements not matching the given predicate'
    },
    {
      label: 'map',
      detail: '(transform: (T) -> R) -> List<R>',
      documentation: 'Returns a list of results of applying the given transform to each element'
    },
    {
      label: 'mapNotNull',
      detail: '(transform: (T) -> R?) -> List<R>',
      documentation: 'Returns a list containing only non-null results of the transform'
    },
    {
      label: 'flatMap',
      detail: '(transform: (T) -> Iterable<R>) -> List<R>',
      documentation: 'Returns a single list of all elements yielded by transform'
    },
    {
      label: 'forEach',
      detail: '(action: (T) -> Unit) -> Unit',
      documentation: 'Performs the given action on each element'
    },
    {
      label: 'forEachIndexed',
      detail: '(action: (Int, T) -> Unit) -> Unit',
      documentation: 'Performs the given action on each element with its index'
    },
    {
      label: 'any',
      detail: '(predicate: (T) -> Boolean) -> Boolean',
      documentation: 'Returns true if at least one element matches the predicate'
    },
    {
      label: 'all',
      detail: '(predicate: (T) -> Boolean) -> Boolean',
      documentation: 'Returns true if all elements match the predicate'
    },
    {
      label: 'none',
      detail: '(predicate: (T) -> Boolean) -> Boolean',
      documentation: 'Returns true if no elements match the predicate'
    },
    {
      label: 'count',
      detail: '(predicate: (T) -> Boolean) -> Int',
      documentation: 'Returns the count of elements matching the predicate'
    },
    {
      label: 'find',
      detail: '(predicate: (T) -> Boolean) -> T?',
      documentation: 'Returns the first element matching the predicate, or null'
    },
    {
      label: 'firstOrNull',
      detail: '(predicate: (T) -> Boolean) -> T?',
      documentation: 'Returns the first element matching the predicate, or null'
    },
    {
      label: 'first',
      detail: '(predicate: (T) -> Boolean) -> T',
      documentation: 'Returns the first element matching the predicate'
    },
    {
      label: 'last',
      detail: '(predicate: (T) -> Boolean) -> T',
      documentation: 'Returns the last element matching the predicate'
    },
    {
      label: 'lastOrNull',
      detail: '(predicate: (T) -> Boolean) -> T?',
      documentation: 'Returns the last element matching the predicate, or null'
    },
    {
      label: 'sortedBy',
      detail: '(selector: (T) -> R) -> List<T>',
      documentation: 'Returns a list sorted by the given selector'
    },
    {
      label: 'sortedByDescending',
      detail: '(selector: (T) -> R) -> List<T>',
      documentation: 'Returns a list sorted descending by the given selector'
    },
    {
      label: 'groupBy',
      detail: '(keySelector: (T) -> K) -> Map<K, List<T>>',
      documentation: 'Groups elements by the given key selector'
    },
    {
      label: 'associate',
      detail: '(transform: (T) -> Pair<K, V>) -> Map<K, V>',
      documentation: 'Returns a Map from the given transform pairs'
    },
    {
      label: 'associateBy',
      detail: '(keySelector: (T) -> K) -> Map<K, T>',
      documentation: 'Returns a Map keyed by the given selector'
    },
    {
      label: 'distinct',
      detail: '() -> List<T>',
      documentation: 'Returns a list with only distinct elements'
    },
    {
      label: 'distinctBy',
      detail: '(selector: (T) -> K) -> List<T>',
      documentation: 'Returns a list with elements having distinct keys'
    },
    {
      label: 'take',
      detail: '(n: Int) -> List<T>',
      documentation: 'Returns a list of the first n elements'
    },
    {
      label: 'drop',
      detail: '(n: Int) -> List<T>',
      documentation: 'Returns a list skipping the first n elements'
    },
    {
      label: 'chunked',
      detail: '(size: Int) -> List<List<T>>',
      documentation: 'Splits the collection into chunks of the given size'
    },
    {
      label: 'joinToString',
      detail: '(separator: String) -> String',
      documentation: 'Creates a string from all elements separated by the given separator'
    },
    { label: 'toList', detail: '() -> List<T>', documentation: 'Converts to a List' },
    { label: 'toSet', detail: '() -> Set<T>', documentation: 'Converts to a Set' },
    {
      label: 'toMutableList',
      detail: '() -> MutableList<T>',
      documentation: 'Converts to a MutableList'
    },
    {
      label: 'sumOf',
      detail: '(selector: (T) -> Int) -> Int',
      documentation: 'Returns the sum of all values produced by the selector'
    },
    {
      label: 'maxOf',
      detail: '(selector: (T) -> R) -> R',
      documentation: 'Returns the maximum value produced by the selector'
    },
    {
      label: 'minOf',
      detail: '(selector: (T) -> R) -> R',
      documentation: 'Returns the minimum value produced by the selector'
    },
    {
      label: 'flatten',
      detail: '() -> List<T>',
      documentation: 'Returns a single list of all elements in nested collections'
    },
    {
      label: 'zip',
      detail: '(other: Iterable<R>) -> List<Pair<T, R>>',
      documentation: 'Returns a list of pairs built from elements at the same positions'
    },
    {
      label: 'windowed',
      detail: '(size: Int) -> List<List<T>>',
      documentation: 'Returns a list of snapshots of the window of given size'
    },
    {
      label: 'plus',
      detail: '(elements: Collection<T>) -> List<T>',
      documentation: 'Returns a list containing all elements of this and the given collection'
    },
    {
      label: 'minus',
      detail: '(elements: Collection<T>) -> List<T>',
      documentation: 'Returns a list containing all elements except those in the given collection'
    },
    {
      label: 'intersect',
      detail: '(other: Iterable<T>) -> Set<T>',
      documentation: 'Returns a set containing elements present in both collections'
    },
    {
      label: 'union',
      detail: '(other: Iterable<T>) -> Set<T>',
      documentation: 'Returns a set of all distinct elements from both collections'
    },
    { label: 'size', detail: 'Int', documentation: 'The number of elements in this collection' },
    {
      label: 'isEmpty',
      detail: '() -> Boolean',
      documentation: 'Returns true if the collection is empty'
    },
    {
      label: 'isNotEmpty',
      detail: '() -> Boolean',
      documentation: 'Returns true if the collection is not empty'
    }
  ];

  // Node properties commonly used in CPG queries
  const cpgNodeProperties = [
    { label: 'name', detail: 'Name', documentation: 'The qualified name of this node' },
    { label: 'localName', detail: 'String', documentation: 'The local (simple) name' },
    {
      label: 'location',
      detail: 'PhysicalLocation?',
      documentation: 'The physical source location'
    },
    { label: 'astParent', detail: 'AstNode?', documentation: 'The parent node in the AST' },
    {
      label: 'astChildren',
      detail: 'List<AstNode>',
      documentation: 'The direct children in the AST'
    },
    { label: 'nextDFG', detail: 'Set<Node>', documentation: 'Next data flow graph edges' },
    { label: 'prevDFG', detail: 'Set<Node>', documentation: 'Previous data flow graph edges' },
    { label: 'nextEOG', detail: 'List<Node>', documentation: 'Next evaluation order graph edges' },
    {
      label: 'prevEOG',
      detail: 'List<Node>',
      documentation: 'Previous evaluation order graph edges'
    },
    { label: 'type', detail: 'Type', documentation: 'The inferred type of this node' },
    {
      label: 'language',
      detail: 'Language<*>?',
      documentation: 'The language this node belongs to'
    },
    { label: 'code', detail: 'String?', documentation: 'The original source code of this node' },
    { label: 'comment', detail: 'String?', documentation: 'A comment attached to this node' },
    { label: 'annotations', detail: 'List<Annotation>', documentation: 'Annotations on this node' }
  ];

  // callsByName extension function
  const cpgExtensionFunctions = [
    {
      label: 'callsByName',
      detail: '(name: String) -> List<Call>',
      documentation: 'Returns all calls to a function with the given name'
    },
    {
      label: 'callersOf',
      detail: '(function: Function) -> Set<Function>',
      documentation: 'Returns all functions that call the given function'
    }
  ];

  onMount(async () => {
    // Dynamically import Monaco to avoid SSR issues
    const monaco = await import('monaco-editor');
    monacoModule = monaco;

    // Configure Monaco worker environment (use editor worker only, no language servers)
    (self as unknown as { MonacoEnvironment: { getWorker: () => Worker } }).MonacoEnvironment = {
      getWorker: () => {
        // Use a blob worker that does nothing - we don't need the full language server
        const blob = new Blob(['self.onmessage = () => {};'], { type: 'application/javascript' });
        return new Worker(URL.createObjectURL(blob));
      }
    };

    // Register Kotlin language if not already registered
    const languages = monaco.languages.getLanguages();
    if (!languages.find((l) => l.id === 'kotlin')) {
      monaco.languages.register({ id: 'kotlin', extensions: ['.kt', '.kts'] });
    }

    // Set Kotlin tokenizer
    monaco.languages.setLanguageConfiguration('kotlin', kotlinLanguageConfig);
    monaco.languages.setMonarchTokensProvider(
      'kotlin',
      kotlinTokensProvider as Parameters<typeof monaco.languages.setMonarchTokensProvider>[1]
    );

    // Register CPG completion provider
    monaco.languages.registerCompletionItemProvider('kotlin', {
      triggerCharacters: ['.'],
      provideCompletionItems(model, position) {
        const word = model.getWordUntilPosition(position);
        const range = {
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
          startColumn: word.startColumn,
          endColumn: word.endColumn
        };

        // Get text before cursor to detect context
        const textBeforeCursor = model.getValueInRange({
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
          startColumn: 1,
          endColumn: position.column
        });

        const suggestions: import('monaco-editor').languages.CompletionItem[] = [];

        // Check if user typed "result."
        if (textBeforeCursor.endsWith('result.')) {
          for (const prop of cpgResultProperties) {
            suggestions.push({
              label: prop.label,
              kind: monaco.languages.CompletionItemKind.Property,
              detail: prop.detail,
              documentation: prop.documentation,
              insertText: prop.label,
              range
            });
          }
          for (const fn of cpgExtensionFunctions) {
            suggestions.push({
              label: fn.label,
              kind: monaco.languages.CompletionItemKind.Method,
              detail: fn.detail,
              documentation: fn.documentation,
              insertText: fn.label + '(',
              range
            });
          }
          return { suggestions };
        }

        // Check if user typed a dot after any expression (offer collection methods)
        if (textBeforeCursor.trimEnd().endsWith('.')) {
          for (const method of kotlinCollectionMethods) {
            suggestions.push({
              label: method.label,
              kind: method.detail.startsWith('(')
                ? monaco.languages.CompletionItemKind.Method
                : monaco.languages.CompletionItemKind.Property,
              detail: method.detail,
              documentation: method.documentation,
              insertText:
                method.detail === 'Int' || method.detail.endsWith('Boolean')
                  ? method.label
                  : method.label + (method.detail.startsWith('()') ? '()' : ' { '),
              range
            });
          }
          for (const prop of cpgNodeProperties) {
            suggestions.push({
              label: prop.label,
              kind: monaco.languages.CompletionItemKind.Property,
              detail: prop.detail,
              documentation: prop.documentation,
              insertText: prop.label,
              range
            });
          }
          return { suggestions };
        }

        // Top-level completions: offer `result` as a variable
        suggestions.push({
          label: 'result',
          kind: monaco.languages.CompletionItemKind.Variable,
          detail: 'TranslationResult',
          documentation:
            'The current analysis result (TranslationResult). Use result.<property> to query.',
          insertText: 'result',
          range
        });

        return { suggestions };
      }
    });

    // Create editor instance
    editorInstance = monaco.editor.create(containerEl, {
      value,
      language: 'kotlin',
      theme: 'vs',
      fontSize: 13,
      fontFamily:
        "'Noto Sans Mono', 'Fira Code', 'Cascadia Code', Consolas, 'Courier New', monospace",
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      automaticLayout: true,
      wordWrap: 'on',
      lineNumbers: 'on',
      glyphMargin: false,
      folding: true,
      lineDecorationsWidth: 4,
      lineNumbersMinChars: 3,
      renderLineHighlight: 'line',
      suggestOnTriggerCharacters: true,
      quickSuggestions: { other: true, comments: false, strings: false },
      acceptSuggestionOnEnter: 'on',
      tabSize: 2,
      insertSpaces: true,
      fixedOverflowWidgets: true
    });

    // Sync changes back to parent
    editorInstance.onDidChangeModelContent(() => {
      if (editorInstance && !isUpdatingFromParent) {
        const newValue = editorInstance.getValue();
        value = newValue;
        onchange?.(newValue);
      }
    });
  });

  // Keep editor in sync when `value` prop changes externally
  $effect(() => {
    if (editorInstance && monacoModule) {
      const currentEditorValue = editorInstance.getValue();
      if (currentEditorValue !== value) {
        isUpdatingFromParent = true;
        const model = editorInstance.getModel();
        if (model) {
          // Use pushEditOperations to preserve undo history
          editorInstance.executeEdits('external', [
            {
              range: model.getFullModelRange(),
              text: value
            }
          ]);
        }
        isUpdatingFromParent = false;
      }
    }
  });

  onDestroy(() => {
    editorInstance?.dispose();
  });
</script>

<div
  bind:this={containerEl}
  style="height: {height}; width: 100%;"
  class="overflow-hidden rounded-md border border-gray-300"
></div>
