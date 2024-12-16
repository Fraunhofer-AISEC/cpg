# There are basically three ways to import things in python, which we will show-case
# in this file

# 1) a regular import of a module "a". To access types and functions, they need to be
#    prefixed with `a`, such as `a.func()`.
import a

# 2) import of a single or multiple symbols of module "b". They individual symbols
#    such as `func` are available without any module prefix
from b import func, other_func

# 3) wildcard import of all symbols of module "c". This behaves identically to 2), but
#    because it imports everything, we only know which symbols are imported at a later
#    stage
from c import *

# Lastly, as a variant, any imported symbol (that is not a wildcard) can also be given
# an alias.
from a import func as a_func
import c as different

# these calls should resolve to module "a"
a.func()
a.other_func()
a_func()

# these calls should resolve to module "b"
func()
other_func()

# these calls should resolve to module "c"
completely_different_func()
different.completely_different_func()

# these should be a nested member call with an "inner" call to a qualified "a.foobar"
a.foobar.bar()
