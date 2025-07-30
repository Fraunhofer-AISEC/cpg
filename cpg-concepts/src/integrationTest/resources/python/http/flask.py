from flask import Flask

app = Flask(__name__)

class Resource:
    def __init__(self, id):
        self.id = id

resources = [Resource("Resource1"), Resource("Resource2")]

@app.route("/resources")
def list_resources():
    return resources


@app.route("/resources/<resource_id>")
def list_resources(resource_id: str):
    # Return a specific resource by ID
    return [resource for resource in resources if resource.id == resource_id][0]
