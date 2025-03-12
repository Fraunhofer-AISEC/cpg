import os

if __name__ == '__main__':
    file = "/tmp/foo.txt"
    fh = open(file, "w+")
    fh.write("Writing to the file before chmod ain't a good idea...")
    os.chmod(file, 0o600)