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
