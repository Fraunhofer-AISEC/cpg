# Svelte Language Integration Guide

This document provides a comprehensive guide on how Svelte language support was implemented in the CPG (Code Property Graph) framework and how to build, test, and use it in external projects.

## Overview

The Svelte language support implementation allows CPG to parse and analyze Svelte component files (`.svelte`) by:
1. **Script Block Parsing**: Extracting and parsing TypeScript/JavaScript from `<script>` tags
2. **HTML Template Analysis**: Processing Svelte template syntax, bindings, and event handlers  
3. **CSS Style Processing**: Analyzing styles from `<style>` blocks
4. **Unified CPG Generation**: Creating a complete code property graph from all component parts

## Architecture

### Core Components

1. **`SvelteLanguage.kt`** - Language definition and file extension registration
2. **`SvelteLanguageFrontend.kt`** - Main parsing frontend that orchestrates Svelte component analysis
3. **`SvelteAST.kt`** - AST node definitions for Svelte-specific structures
4. **`TypeScriptLanguageFrontend.kt`** - Enhanced with Svelte file detection and delegation
5. **`parser.ts`** - TypeScript/Deno parser with Svelte compiler integration

### Integration Points

- **Language Registration**: Added to `cpg-neo4j/Application.kt` for tool recognition
- **Frontend Delegation**: TypeScript frontend detects `.svelte` files and delegates to Svelte frontend
- **Parser Extension**: TypeScript parser supports `--language=svelte` flag for Svelte compiler usage

## Implementation Details

### 1. Language Definition (`SvelteLanguage.kt`)

```kotlin
class SvelteLanguage : Language<SvelteLanguageFrontend>() {
    override val fileExtensions = listOf("svelte")
    override val namespaceDelimiter = "."
    override val frontend = SvelteLanguageFrontend::class
    override val qualifiedSuperTypeDelimiter = "."
}
```

### 2. Frontend Implementation (`SvelteLanguageFrontend.kt`)

Key parsing methods:
- `handleSvelteProgram()` - Main entry point for Svelte component processing
- `handleScriptBlock()` - Processes `<script>` content using TypeScript parsing
- `handleTemplateBlock()` - Analyzes HTML template with Svelte bindings  
- `handleCssBlock()` - Parses CSS styles and creates style declarations

### 3. TypeScript Parser Extension (`parser.ts`)

```typescript
if (language === "svelte") {
    ast = svelteParse(fileContent, { filename: file });
} else if (language === "typescript") {
    // Original TypeScript parsing...
}
```

## Build Process

### Prerequisites

- Java 17+
- Gradle 8.13+
- Deno runtime (for TypeScript parser)

### Building the CPG Tool

1. **Build the TypeScript Language Module**:
   ```bash
   cd cpg
   ./gradlew :cpg-language-typescript:build
   ```

2. **Build the Complete CPG Distribution**:
   ```bash
   ./gradlew :cpg-neo4j:installDist
   ```

3. **Verify Build**:
   ```bash
   ls -la cpg-neo4j/build/install/cpg-neo4j/
   # Should contain: bin/ lib/ (with cpg-language-typescript.jar)
   ```

### Generated Distribution Structure

```
cpg-neo4j/build/install/cpg-neo4j/
├── bin/
│   ├── cpg-neo4j          # Main executable
│   └── cpg-neo4j.bat      # Windows executable
└── lib/
    ├── cpg-language-typescript.jar  # Contains Svelte support
    ├── cpg-core.jar
    └── [other dependencies...]
```

## Testing Svelte Support

### 1. Unit Tests

Run the comprehensive test suite:

```bash
./gradlew :cpg-language-typescript:test --tests "*SvelteLanguageFrontendTest*"
```

Expected test coverage:
- ✅ Script block parsing (variables, functions, expressions)
- ✅ HTML template processing (elements, bindings, events)  
- ✅ CSS style block analysis (rules, selectors, properties)
- ✅ JSON output generation for visualization

### 2. Integration Testing

Create a test Svelte component:

```svelte
<!-- SimpleComponent.svelte -->
<script lang="ts">
    export let name: string = "World";
    let count: number = 0;
    
    function handleClick() {
        count += 1;
    }
</script>

<h1>Hello {name}!</h1>
<p>You've clicked the button {count} times.</p>
<button on:click={handleClick}>
    Click me
</button>

<style>
    h1 {
        color: purple;
    }
</style>
```

Test with CPG tool:

```bash
./cpg-neo4j/bin/cpg-neo4j --export-json=output.json --no-neo4j SimpleComponent.svelte
```

Expected output indicators:
- ✅ `"Executing SvelteLanguageFrontend for..."` in logs
- ✅ JSON file containing parsed variables, functions, and CSS declarations
- ✅ No "Found no parser frontend" warnings

### 3. JSON Output Verification

The generated JSON should contain:

```json
{
  "nodes": [
    {"labels": ["VariableDeclaration"], "properties": {"name": "name"}},
    {"labels": ["VariableDeclaration"], "properties": {"name": "count"}},
    {"labels": ["FunctionDeclaration"], "properties": {"name": "handleClick"}},
    {"labels": ["RecordDeclaration"], "properties": {"kind": "css_stylesheet"}}
  ],
  "edges": [...]
}
```

## External Project Integration

### Example: cpg-wrapper-service Integration

#### 1. Copy CPG Distribution

```bash
# From CPG project root
cp -r cpg-neo4j/build/install/cpg-neo4j /path/to/external-project/cpg-tool-dist/
```

#### 2. Test Svelte Parsing

```bash
cd /path/to/external-project/cpg-tool-dist
./cpg-neo4j/bin/cpg-neo4j --export-json=svelte-analysis.json --no-neo4j my-component.svelte
```

#### 3. Verification Steps

1. **Check Recognition**: Logs should show "Executing SvelteLanguageFrontend"
2. **Verify Parsing**: JSON output should contain component structures
3. **Test Integration**: Use JSON in your visualization/analysis pipeline

### Common Integration Issues

#### Issue: "Found no parser frontend for .svelte files"

**Cause**: SvelteLanguage not registered in Application.kt

**Solution**: Ensure the following line exists in `cpg-neo4j/Application.kt`:
```kotlin
.optionalLanguage("de.fraunhofer.aisec.cpg.frontends.typescript.SvelteLanguage")
```

#### Issue: TypeScript compilation errors

**Cause**: Missing TypeScript parser dependencies

**Solution**: Verify `cpg-language-typescript.jar` contains:
- Deno TypeScript parser binary
- Svelte compiler integration
- All required dependencies

## Development Workflow

### Adding New Svelte Features

1. **Update AST Definitions** in `SvelteAST.kt`
2. **Extend Parser Logic** in `SvelteLanguageFrontend.kt`
3. **Add Parser Support** in `parser.ts` if needed
4. **Write Tests** in `SvelteLanguageFrontendTest.kt`
5. **Test Integration** with JSON output verification

### Debugging Tips

1. **Enable Debug Logging**:
   ```bash
   ./cpg-neo4j/bin/cpg-neo4j --export-json=debug.json --no-neo4j test.svelte 2>&1 | tee debug.log
   ```

2. **Check Parser Binary**:
   ```bash
   jar -tf lib/cpg-language-typescript.jar | grep parser
   ```

3. **Verify Language Registration**:
   ```bash
   # Should show SvelteLanguage in available languages
   ./cpg-neo4j/bin/cpg-neo4j --help
   ```

## Performance Considerations

### Build Optimization

- Use `--parallel` flag for faster Gradle builds
- Consider `--build-cache` for incremental builds
- Test individual modules: `./gradlew :cpg-language-typescript:build`

### Runtime Performance

- Use `--no-neo4j` flag for JSON-only output (faster)
- Consider `--max-complexity-cf-dfg` for large codebases
- Limit recursion with `--save-depth` for complex graphs

## Future Enhancements

### Planned Features

1. **Advanced Svelte Syntax**: Support for stores, reactive statements (`$:`)
2. **SvelteKit Integration**: Route analysis and layout processing
3. **Component Dependencies**: Cross-component relationship mapping
4. **Enhanced CSS Analysis**: Scoped styles and CSS custom properties

### Extension Points

- **Custom Passes**: Add Svelte-specific analysis passes
- **Enhanced AST**: Extend AST nodes for advanced Svelte constructs
- **Tool Integration**: IDE plugins and linting tool support

## Troubleshooting

### Build Issues

**Gradle Build Fails**:
- Check Java version: `java -version` (requires Java 17+)
- Clean build: `./gradlew clean build`
- Check dependencies: `./gradlew dependencies`

**TypeScript Parser Missing**:
- Verify resource extraction in TypeScriptLanguageFrontend
- Check platform-specific parser binary availability
- Ensure executable permissions on extracted parser

### Runtime Issues

**JSON Output Empty**:
- Verify SvelteLanguage registration
- Check file extension recognition
- Enable debug logging for detailed parsing information

**Parsing Errors**:
- Validate Svelte component syntax
- Check TypeScript/JavaScript code in `<script>` blocks
- Verify CSS syntax in `<style>` blocks

## Additional Resources

- [CPG Documentation](https://fraunhofer-aisec.github.io/cpg/)
- [Svelte Compiler API](https://svelte.dev/docs/svelte-compiler)
- [TypeScript Parser Documentation](https://github.com/microsoft/TypeScript/wiki/Using-the-Compiler-API)

---

*This guide covers the complete implementation and usage of Svelte language support in CPG. For additional questions or contributions, please refer to the project's issue tracker and contribution guidelines.* 