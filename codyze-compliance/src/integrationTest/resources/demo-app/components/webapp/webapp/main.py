"""
Simulates the execution of a command line tool
"""
def execute(command, *args, stdin=None):
    pass

"""
Simulates the retrieval of a secret from a server
"""
def get_secret_from_server() -> str:
    pass


def encrypt():
    my_secret = get_secret_from_server()
    execute("encrypt",
            "--very-good",
            stdin=my_secret)
    del my_secret
    return

def decrypt():
    my_secret = get_secret_from_server()
    execute("decrypt",
            "--very-good",
            stdin=my_secret)
    del my_secret
    return
