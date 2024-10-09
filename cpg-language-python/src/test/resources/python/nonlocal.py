def outer_function():
    c = 1

    def nonlocal_write():
        nonlocal c
        c = 2
        print(c)

    def nonlocal_read():
        print(c)

    def local_write():
        c = 2
        print(c)

    def nonlocal_error():
        c = c + 1
