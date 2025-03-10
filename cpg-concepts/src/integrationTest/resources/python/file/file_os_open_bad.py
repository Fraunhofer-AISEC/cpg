import os

flags = os.O_WRONLY | os.O_CREAT | os.O_TRUNC
with os.open('example.txt', flags, 0o600) as fh:
    fh.write('foo')
    os.chmod('example.txt', 0o511)
