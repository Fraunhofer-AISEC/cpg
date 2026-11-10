/**
 * Splits the HTML produced by the syntax highlighter into one HTML string per source line.
 *
 * A highlighted `<span>` may wrap a line break (e.g. a block comment). Such spans are closed at
 * the end of the line and reopened at the start of the next one, so that every line is valid HTML
 * on its own and can be rendered independently.
 *
 * @param html The highlighted HTML of the whole file.
 * @returns The highlighted HTML of each line.
 */
export function splitHighlightedLines(html: string): string[] {
  const lines: string[] = [];
  const openTags: string[] = [];
  let current = '';
  let pos = 0;
  let tagAt = html.indexOf('<');
  let breakAt = html.indexOf('\n');

  while (tagAt !== -1 || breakAt !== -1) {
    if (breakAt !== -1 && (tagAt === -1 || breakAt < tagAt)) {
      current += html.slice(pos, breakAt) + '</span>'.repeat(openTags.length);
      lines.push(current);
      current = openTags.join('');
      pos = breakAt + 1;
      breakAt = html.indexOf('\n', pos);
    } else {
      const tagEnd = html.indexOf('>', tagAt) + 1;
      const tag = html.slice(tagAt, tagEnd);
      if (tag.startsWith('</')) {
        openTags.pop();
      } else {
        openTags.push(tag);
      }
      current += html.slice(pos, tagEnd);
      pos = tagEnd;
      tagAt = html.indexOf('<', pos);
    }
  }

  lines.push(current + html.slice(pos));
  return lines;
}

/**
 * Declarations of constants that Ghidra names after the text of a string, e.g.
 * `string s_(cpu_divider_<=_ahb_divider)_&&_(_ram_40818890;`.
 */
const STRING_DECLARATION = /^(?:string|unicode)[ \t]+\S+;[ \t\r]*$/;

/**
 * Prepares the lines of a file for the syntax highlighter. The names of the string constants of
 * decompiled code can contain any character of the string. The highlighter reads such a line as
 * the head of a function, and an unbalanced parenthesis in the name keeps its parameter list open
 * until the end of the file.
 *
 * The characters of these names are replaced, which keeps the length of every line. The original
 * text is put back into the highlighted line afterwards (see [restoreText]).
 *
 * @param lines The lines of the file.
 * @returns The code to highlight, and the original text of the lines that were replaced by their
 *   index.
 */
export function maskUnparsableNames(lines: string[]): { code: string; plain: Map<number, string> } {
  const plain = new Map<number, string>();
  const masked = lines.map((line, i) => {
    if (!STRING_DECLARATION.test(line)) return line;
    plain.set(i, line);
    return line.replace(/[^A-Za-z0-9_;\s]/g, '_');
  });
  return { code: plain.size > 0 ? masked.join('\n') : lines.join('\n'), plain };
}

/** Escapes text the way the syntax highlighter does, to show it as HTML. */
export function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;');
}

/**
 * Puts the original text into the highlighted HTML of a line that was highlighted with other
 * characters of the same length (see [maskUnparsableNames]), keeping its tags.
 */
export function restoreText(html: string, original: string): string {
  let result = '';
  let index = 0;
  let pos = 0;
  while (pos < html.length) {
    if (html[pos] === '<') {
      const end = html.indexOf('>', pos) + 1;
      result += html.slice(pos, end);
      pos = end;
    } else {
      result += escapeHtml(original[index++] ?? '');
      pos++;
    }
  }
  return result;
}
