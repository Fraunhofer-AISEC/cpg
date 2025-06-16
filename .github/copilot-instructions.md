# GitHub Copilot Custom Instructions for CPG Project

## Project Overview
This is the CPG (Code Property Graph) project ecosystem, a comprehensive platform for code analysis and security compliance. The project consists of multiple modules working together to provide static analysis capabilities.

## Project Structure
- **cpg-core**: Core CPG functionality and data structures
- **cpg-language-***: Language-specific frontends (C++, Java, Python, Go, etc.)
- **cpg-analysis**: Analysis engines and algorithms
- **cpg-concepts**: Concept definitions and compliance rules
- **codyze-console**: Web-based interface for project analysis
- **codyze-compliance**: Compliance checking and reporting
- **codyze**: CLI tool for analysis

## Package Management
- **Always use `pnpm` for Node.js/JavaScript/TypeScript projects**
- Use `pnpm install` instead of `npm install`
- Use `pnpm add` instead of `npm install <package>`
- Use `pnpm run` for running scripts

## Module-Specific Guidelines

### codyze-console Module

#### Technologies
- **Backend**: Kotlin with Spring Boot
- **Frontend**: Svelte 5 with SvelteKit
- **Styling**: Tailwind CSS
- **Package Manager**: pnpm

#### Svelte 5 Runes
Always use Svelte 5 runes syntax:
- Use `let variableName = $state(initialValue)` for reactive state
- Use `const derivedValue = $derived(expression)` for computed values
- Use `$effect(() => { ... })` for side effects
- Use `let { prop1, prop2 }: Props = $props()` for component props

#### SvelteKit Load Pattern with Runes
For SvelteKit's load functionality with Svelte 5 runes:

1. Define data loading in `+page.ts` files:
```ts
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch }) => {
  const response = await fetch('/api/endpoint');
  const data = await response.json();
  return { data };
};
```

2. Access loaded data in `+page.svelte` using PageProps:
```svelte
<script lang="ts">
  import type { PageProps } from './$types';
  
  // Correctly access data with $props()
  let { data }: PageProps = $props();
  
  // Use runes for derived values based on that data
  const items = $derived(data.items || []);
</script>
```

#### Component Structure
- Use clean separation of components
- Keep components reusable and modular
- Use a clean, minimal design with limited shadows
- Use Tailwind CSS for styling
- Follow accessibility best practices (use proper semantic HTML, ARIA roles)

#### Event Handling
- Use modern Svelte 5 event syntax: `onclick` instead of `on:click`
- Use proper button elements for interactive content instead of clickable divs


#### Building and Testing
- Use `./gradlew :codyze-console:compileKotlin --console=plain` for checking Kotlin compilation errors in the codyze-console module from the root project directory.
- For building the backend, use `./gradlew :codyze-console:compileKotlin --console=plain` from the root project directory.
- Starting the backend is more complicated, please just ask me to do it.

#### Known Issues & Workarounds
- **svelte-highlight compatibility**: The current version doesn't support Svelte 5 runes mode. Consider alternatives or temporary workarounds until the package is updated.

### CPG Core Modules (cpg-core, cpg-language-*, cpg-analysis)

#### Technologies
- **Language**: Kotlin
- **Build Tool**: Gradle with Kotlin DSL
- **Testing**: JUnit 5, Mockk
- **Documentation**: KDoc

#### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Write comprehensive KDoc comments for public APIs
- Prefer immutable data structures where possible
- Use sealed classes for representing state and results

#### Testing
- Write unit tests for all public APIs
- Use descriptive test method names
- Follow AAA pattern (Arrange, Act, Assert)
- Mock external dependencies appropriately

### Codyze CLI Module

#### Technologies
- **Language**: Kotlin
- **CLI Framework**: Clikt
- **Configuration**: YAML/JSON

#### CLI Design
- Provide clear help messages and examples
- Use consistent command naming
- Support both short and long option names
- Validate input parameters early with meaningful error messages

### General Guidelines

#### Git Workflow
- Use conventional commit messages
- Create feature branches for new functionality
- Write descriptive commit messages
- Include relevant issue numbers in commits

#### Documentation
- Keep README files up to date
- Document API changes in appropriate files
- Use clear examples in documentation
- Include troubleshooting sections where relevant

#### Error Handling
- Provide meaningful error messages
- Log appropriate information for debugging
- Handle edge cases gracefully
- Use appropriate exception types


#### Prompt Output

- I do not need a detailed summary of the changes you made. You can be brief about this.
