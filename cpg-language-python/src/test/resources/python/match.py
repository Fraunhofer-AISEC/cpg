def matcher(x):
    match x:
        case None:
            print("singleton" + x)
        case "value":
            print("value" + x)
        case [x] if x>0:
            print(x)
        case [1, 2]:
            print("sequence" + x)
        case [1, 2, *rest]:
            print("star" + x)
        case [*_]:
            print("star2" + x)
        case {1: _, 2: _}:
            print("mapping" + x)
        case Point2D(0, 0):
            print("class" + x)
        case [x] as y:
            print("as" + y)
        case [x] | (y):
            print("or" + x)
        case _:
            print("Default match")