import type {ConceptInfo} from "$lib/types";

export interface ConceptGroup {
  path: string;
  concepts: { name: string; fullName: string }[];
}

export function groupConcepts(concepts: ConceptInfo[]): ConceptGroup[] {
  const prefix = 'de.fraunhofer.aisec.cpg.graph.concepts.';
  const groups: Map<string, { name: string; fullName: string }[]> = new Map();

  for (const fullInfo of concepts) {
    const fullName = fullInfo.conceptName
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
