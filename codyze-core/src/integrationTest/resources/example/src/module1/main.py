from library.lib import my_func

key = get_secret_from_server()

err = encrypt("Hello World", key, cipher = "AES-256")
my_func()
