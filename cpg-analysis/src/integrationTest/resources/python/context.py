def endpoint1():
    policy = "endpoint1"
    # looks good
    authorize(policy)


def endpoint2():
    policy = "endpoint2"
    # looks good
    authorize(policy)


def endpoint3():
    policy = "endpoint4"
    # uh-uh, this is not good
    authorize(policy)


# This is a helper function to simulate the authorization process
def authorize(policy: str) -> bool:
    # This function simulates the authorization logic. The
    # policy name should match the endpoint where it's coming from.
    inner_authorize(policy)
