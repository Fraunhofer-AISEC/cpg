"""
Tiny Python demo for the CPG REPL — same shape as the C demo so we can
run the same queries cross-language.

Theme: untrusted input flowing into a dangerous sink.
"""
import configparser
import logging
import os

# Configure logging - demonstrates PythonLoggingConceptPass
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


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


# === Demo: Configuration concepts (PythonStdLibConfigurationPass) ===
def load_config():
    """Load configuration from config.ini - demonstrates Configuration concepts."""
    config = configparser.ConfigParser()
    config.read('config.ini')

    api_key = config.get('api', 'key')
    api_secret = config.get('api', 'secret')

    logger.info(f"Loaded API key: {api_key}")
    logger.error("Failed to validate configuration")

    return api_key, api_secret


# === Demo: File operations (PythonFileConceptPass) ===
def write_output(data: str):
    """Write data to output file - demonstrates File concepts."""
    with open("output.txt", "w") as f:
        f.write(f"Data: {data}\n")


# === Demo: Secret for manual tagging (via YAML) ===
# This variable will be tagged as a Secret concept using app.concepts.yaml
MY_SECRET = "manual_secret_value_12345"


def log_event(message: str) -> None:
    """Utility to persist an event. Intended to use the logger, but
    accidentally writes to a file instead of calling logger.info()."""
    with open("events.log", "a") as f:
        f.write(message + "\n")


def format_debug(tag: str, value: str) -> str:
    """Format a debug message with a tag prefix."""
    return f"[DEBUG] {tag}: {value}"


def demonstrate_secret_flow():
    """Shows a secret leaking to a file through a chain of calls."""
    logger.warning("Processing secret data")
    msg = format_debug("secret", MY_SECRET)
    log_event(msg)


if __name__ == "__main__":
    logger.info("Application starting...")

    # Run original demos
    vulnerable()
    safe()
    vulnerable_with_indirection()

    # Run new concept demos
    api_key, api_secret = load_config()
    write_output(api_key)

    # Demonstrate secret flow
    demonstrate_secret_flow()

    logger.info("Application completed.")