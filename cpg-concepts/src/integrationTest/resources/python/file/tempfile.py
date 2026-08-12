import os
import tempfile

# behaves as mkstemp would:
# - 0600 permissions
# - write owner only
# behaves like a file object
# has no name
# ceases to exist on close
with tempfile.TemporaryFile() as tmp:
    tmp.write('foo')

tmp = tempfile.NamedTemporaryFile(delete=True)
try:
    tmp.write('bar')
finally:
    tmp.close()  # deletes the file
