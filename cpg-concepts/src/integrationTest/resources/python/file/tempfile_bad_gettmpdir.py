import os
import tempfile

tmp = os.path.join(tempfile.gettempdir(), "foo")
if not os.path.exists(tmp):
    with open(tmp, "w") as file:
        file.write("bar")
else:
    pass # handle this
