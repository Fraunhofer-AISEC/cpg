/** Whether a keyboard event happens while the user types, e.g. in an input or a text area. */
export function isTyping(event: KeyboardEvent): boolean {
  const target = event.target as HTMLElement | null;
  if (!target) return false;
  return (
    target.isContentEditable ||
    target.tagName === 'INPUT' ||
    target.tagName === 'TEXTAREA' ||
    target.tagName === 'SELECT'
  );
}

/** Whether the platform modifier (Cmd on macOS, Ctrl elsewhere) is pressed. */
export function hasModifier(event: KeyboardEvent): boolean {
  return event.metaKey || event.ctrlKey;
}
