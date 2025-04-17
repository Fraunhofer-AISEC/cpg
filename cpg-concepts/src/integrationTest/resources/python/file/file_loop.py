x = ["a", "b", "c"]
file = open("a")
for bla in x:
    file.write(bla)
    file = open("b")
