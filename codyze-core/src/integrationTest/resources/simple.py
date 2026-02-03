def foo() -> int:
    bar = 42

    if bar < 20:
        raise "bar is too small"

    return bar
