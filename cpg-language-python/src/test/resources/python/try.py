def tryAll(a):
    try:
        b = a+2
    except Exception1:
        print("There was an occurrence of Exception1")
    except OtherException as e:
        print("We saw exception" + e)
    except:
        print("Catch all!")
    else:
        print("All good, got " + b)
    finally:
        print("It's over")