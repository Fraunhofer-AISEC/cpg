class Client:
    def send(self, data):
        return data


class Service:
    def __init__(self):
        self.client = Client()

    def send_foo(self):
        result = self.client.send("foo")
        return result


class Service2:
    def __init__(self, client: Client):
        self.client = client or Client()

    def send_bar(self):
        result = self.client.send("bar")
        return result