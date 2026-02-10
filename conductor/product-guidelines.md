# Product Guidelines

## Voice and Tone
- **Academic and Formal:** Maintain technical precision and rigor, reflecting the project's research-oriented roots and the complexity of program analysis.
- **Pragmatic and Developer-Centric:** Balance formality with practical clarity, ensuring that documentation provides clear, actionable examples for real-world integration.

## Documentation Standards
- **Precision First:** Use exact terminology when referring to graph theory concepts (e.g., AST, CFG, DFG) or specific language constructs.
- **Contextual Examples:** Every major feature or analysis "Pass" should include a minimal source code snippet and a description of the resulting graph structure.
- **Completeness:** Documentation must cover edge cases, especially how the "forgiving" parser handles specific syntax errors or missing dependencies.

## Design and Visual Identity
- **Clarity over Flourish:** Visual representations of graphs (in documentation or via Neo4j) should prioritize readability and the clear distinction between different node and edge types.
- **Consistency:** Use a consistent set of labels and colors for node types (e.g., `Declaration`, `Statement`, `Expression`) across all visualizations and documentation.

## Communication Principles
- **Transparency:** Clearly communicate the "experimental" status of specific language frontends.
- **Openness:** Encourage community contributions by providing clear architectural overviews and contribution guides that lower the barrier to entry for adding new languages or passes.
