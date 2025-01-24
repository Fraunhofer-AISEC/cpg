key = get_secret_from_server()

err = encrypt("Hello World", key, cipher = "AES-256")
if err:
    print("Some error occurred")
    del key
else:
    del key
