import sys

if sys.platform == "win32":
    def the_func():
        return 1
elif sys.platform != "linux":
    def the_func():
        return "not win32 but also not linux"
else:
    def the_func():
        return "maybe linux"

the_func()

weird_compare = sys.platform == sys.version_info
weird_compare = sys.platform != sys.version_info