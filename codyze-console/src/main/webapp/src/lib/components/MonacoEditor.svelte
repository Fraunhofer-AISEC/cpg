<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import {
    cpgResultProperties,
    kotlinCollectionMethods,
    cpgNodeProperties
  } from '$lib/utils/cpg-completions';

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
              kind:
                prop.kind === 'function'
                  ? monaco.languages.CompletionItemKind.Method
                  : monaco.languages.CompletionItemKind.Property,
              detail: prop.detail,
              documentation: prop.documentation,
              insertText: prop.kind === 'function' ? prop.label + '(' : prop.label,
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
