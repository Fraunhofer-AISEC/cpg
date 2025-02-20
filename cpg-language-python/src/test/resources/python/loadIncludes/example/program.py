import json.encoder

a=json.loads('["foo", {"bar":["baz", null, 1.0, 2]}]')
b=json.encoder.JSONEncoder().item_separator
c=str(1)