/** A successful fuzzy match: higher scores are better matches. */
export interface FuzzyMatch {
  score: number;
  /** The indices of the matched characters in the text, for highlighting */
  indices: number[];
}

const separators = new Set(['/', '\\', '.', '_', '-', ' ', ':']);

/**
 * Matches [query] against [text] as a subsequence, ignoring case, like the quick open of editors:
 * `cvw` matches `CodeViewer`. Consecutive characters and characters at the start of a word (after a
 * separator or at a camel case boundary) score higher. Returns null if the query does not match.
 */
export function fuzzyMatch(query: string, text: string): FuzzyMatch | null {
  const q = query.toLowerCase().replace(/\s+/g, '');
  if (!q) return { score: 0, indices: [] };
  // Jumping ahead to word starts gives better matches, but can miss a match that exists
  return match(q, text, true) ?? match(q, text, false);
}

function match(q: string, text: string, preferWordStarts: boolean): FuzzyMatch | null {
  const t = text.toLowerCase();
  const indices: number[] = [];
  let score = 0;
  let previous = -2;
  let ti = 0;
  for (const char of q) {
    // Prefer an occurrence right after the previous match or at the start of a word
    let found = t.indexOf(char, ti);
    if (found === -1) return null;
    for (let i = found; preferWordStarts && i !== -1; i = t.indexOf(char, i + 1)) {
      if (i === previous + 1) {
        found = i;
        break;
      }
      if (isWordStart(text, i)) {
        found = i;
        break;
      }
    }
    if (found === previous + 1) score += 5;
    if (isWordStart(text, found)) score += 3;
    score += 1;
    indices.push(found);
    previous = found;
    ti = found + 1;
  }
  // Shorter texts are better matches for the same query
  score -= text.length * 0.01;
  return { score, indices };
}

function isWordStart(text: string, i: number): boolean {
  if (i === 0) return true;
  const previous = text[i - 1];
  if (separators.has(previous)) return true;
  return previous === previous.toLowerCase() && text[i] !== text[i].toLowerCase();
}
