import sys

if sys.platform == "win32":
    def the_func():
        return 1
else:
    def the_func():
        return "not win32"

the_func()
