class Client:
    def send(self, data):
        return data


class Service:
    def __init__(self):
        self.client = Client()

    def do_work(self):
        result = self.client.send("hello")
        return result