import os
import tempfile

fd, path = tempfile.mkstemp()

tmp = os.fdopen(fd, 'w')
tmp.write('stuff')


path = os.path.join(tempfile.mkdtemp(), 'foobarbaz')

tmp = open(path, "w")
tmp.write("hello world!")
