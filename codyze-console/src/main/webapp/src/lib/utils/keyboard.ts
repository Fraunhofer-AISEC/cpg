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

/** The name of the platform modifier for shortcut hints, `⌘` on macOS and `Ctrl+` elsewhere. */
export function modifierLabel(): string {
  if (typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform)) return '⌘';
  return 'Ctrl+';
}
