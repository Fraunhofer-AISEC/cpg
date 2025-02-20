from __future__ import annotations


class Number:
    i: int

    def __init__(self, i):
        self.i = i

    def __add__(self, other: Number) -> Number:
        return Number(self.i + other.i)

    def __pos__(self) -> str:
        return "python is quite crazy"


def main():
    a = Number(5)
    b = Number(10)
    c = a + b
    print(c)

    d = +b
    print(d)

    # the following unfortunately does not work yet because the types of a and b does not seem to propagate to c
    # currently yet.
    d = +c


if __name__ == "__main__":
    main()
