key = get_secret_from_server()

err = encrypt("Hello World", key, cipher = "AES-256")
if err:
    print("Some error occurred")
    del key
# We cannot end the EOG without anything because the EOG pass won't generate
# an EOG for a missing else branch if there's nothing happening afterward.
print("Do something")
