with open("file.txt", "r") as file:
    data = file.read()

with open("file.txt", "r"):
    pass

class TestContextManager:
    def __enter__(self):
        print("Entering context")

    def __exit__(self, exc_type, exc_value, traceback):
        print("Exiting context")

def test_multiple():
    with A() as a, B(a) as b, C(a, b) as c:
        doSomething(a, c)

def test_function():
    with TestContextManager() as cm:
        print("Inside with block")