from library.lib import special_func

key = get_secret_from_server()

err = encrypt("Hello World", key, cipher = "AES-256")
special_func()
