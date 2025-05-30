import os
import tempfile

fd, path = tempfile.mkstemp()

tmp = os.fdopen(fd, 'w')
tmp.write('stuff')

os.remove(path)

tmpdir = tempfile.mkdtemp()
predictable_filename = 'foobarbaz'

path = os.path.join(tmpdir, predictable_filename)

tmp = open(path, "w")
tmp.write("secrets!")
