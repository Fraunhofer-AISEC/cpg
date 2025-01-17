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
        case "xyz" | "abc":
            print("or" + x)
        case _:
            print("Default match")

def matchSingleton(x):
    match x:
        case None:
            print("singleton" + x)

def matchValue(x):
    match x:
        case "value":
            print("value" + x)

def matchOr(x):
    match x:
        case "xyz" | "abc":
            print("or" + x)

def matchAnd(x):
    match x:
        case [x] if x>0:
            print(x)

def matchDefault(x):
    match x:
        case _:
            print("Default match")

def match_weird():
    match command.split():
        case ["go", ("north" | "south" | "east" | "west") as direction]:
            current_room = current_room.neighbor(direction)