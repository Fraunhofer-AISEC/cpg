def function():
    d = {
        "a": "b",
        "c": "d",
        "e": "f"
    }

    # Test DFGs for dict accesses
    d['b'] = 1
    print(d['b'])
    print(d['a'])
    print(d)
