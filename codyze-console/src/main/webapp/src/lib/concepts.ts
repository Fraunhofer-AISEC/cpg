import type {ConceptInfo} from "$lib/types";

export interface ConceptGroup {
  path: string;
  concepts: { name: string; info: ConceptInfo }[];
}

export function groupConcepts(concepts: ConceptInfo[]): ConceptGroup[] {
  const prefix = 'de.fraunhofer.aisec.cpg.graph.concepts.';
  const groups: Map<string, { name: string; info: ConceptInfo }[]> = new Map();

  for (const info of concepts) {
    const fullName = info.conceptName
    if (!fullName.startsWith(prefix)) continue;

    const name = fullName.split('.').pop()!;
    const path = fullName.slice(prefix.length, fullName.lastIndexOf('.')); // Remove prefix and class name

    if (!groups.has(path)) {
      groups.set(path, []);
    }
    groups.get(path)!.push({ name, info });
  }

  return Array.from(groups.entries())
    .map(([path, concepts]) => ({ path, concepts }))
    .sort((a, b) => a.path.localeCompare(b.path));
}
