def raise_in_try():
    try:
        raise Exception()
    except Exception:
        print("Caught Exception")
    except A:
        print("Caught A")

def raise_in_try2():
    try:
        raise Exception()
    except A:
        print("Caught A")
    except Exception:
        print("Caught Exception")


def raise_without_try():
    raise Exception()

def raise_empty():
    raise

def raise_with_parent():
    raise Exception() from A()
