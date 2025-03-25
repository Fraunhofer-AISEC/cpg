export interface ConceptGroup {
  path: string;
  concepts: { name: string; fullName: string }[];
}

export function groupConcepts(concepts: string[]): ConceptGroup[] {
  const prefix = 'de.fraunhofer.aisec.cpg.graph.concepts.';
  const groups: Map<string, { name: string; fullName: string }[]> = new Map();

  for (const fullName of concepts) {
    if (!fullName.startsWith(prefix)) continue;

    const name = fullName.split('.').pop()!;
    const path = fullName.slice(prefix.length, fullName.lastIndexOf('.')); // Remove prefix and class name

    if (!groups.has(path)) {
      groups.set(path, []);
    }
    groups.get(path)!.push({ name, fullName });
  }

  return Array.from(groups.entries())
    .map(([path, concepts]) => ({ path, concepts }))
    .sort((a, b) => a.path.localeCompare(b.path));
}
