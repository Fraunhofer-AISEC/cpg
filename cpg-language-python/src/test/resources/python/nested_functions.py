def level1():
    c = 1
    def level2():
        d = 1
        def level3():
            nonlocal c, d
            c = c + 2
            d = d + 1
            print(c)

        level3()
    level2()
level1()