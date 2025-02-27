def function():
    d = [
        "b",
        "d",
        "f"
    ]

    # Test DFGs for array accesses
    d[1] = 10
    print(d[1])
    print(d[0])
    print(d)
