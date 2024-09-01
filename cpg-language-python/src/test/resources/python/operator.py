from __future__ import annotations


class Number:
    i: int

    def __init__(self, i):
        self.i = i

    def __add__(self, other: Number) -> Number:
        return Number(self.i + other.i)


def main():
    a = Number(5)
    b = Number(10)
    c = a + b
    print(c)


if __name__ == "__main__":
    main()
