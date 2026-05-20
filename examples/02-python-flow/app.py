"""
Tiny Python demo for the CPG REPL — same shape as the C demo so we can
run the same queries cross-language.

Theme: untrusted input flowing into a dangerous sink.
"""
import os


def get_user_input() -> str:
    """Pretend this came from an HTTP request, env var, …"""
    return os.environ.get("USER_INPUT", "")


def run_command(cmd: str) -> None:
    """Dangerous sink — executes whatever string is passed."""
    os.system(cmd)


def vulnerable():
    """Untrusted input flows directly into os.system — classic command injection."""
    user = get_user_input()
    run_command(user)               # BUG: no sanitisation


def safe():
    """Same shape, but the value is a literal — no flow from user input."""
    run_command("ls -la")


def vulnerable_with_indirection():
    """Same bug as `vulnerable`, but with a renamed intermediate.
    The CPG's data-flow analysis follows the value across the rename. """
    raw = get_user_input()
    sanitised_but_not_really = raw  # not actually sanitised
    run_command(sanitised_but_not_really)


if __name__ == "__main__":
    vulnerable()
    safe()
    vulnerable_with_indirection()
