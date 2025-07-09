import os
import tempfile

if foo():
    tmp = os.path.join(tempfile.gettempdir(), "foo")
else:
    tmp = '/not/a/tmp/file'

file = open(tmp, "w")
file.write("bar")
