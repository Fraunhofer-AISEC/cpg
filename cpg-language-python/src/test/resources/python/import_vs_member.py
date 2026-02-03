from pkg import some_variable, function, third_module
import another_pkg
import another_pkg as alias
import pkg.third_module

# Here we should assume, that this is a member access of the field "field" on base "pkg.some_variable".
# We cannot see with certainty what "some_variable" is, but since we do not see any other import to it
# we can assume that it's not a module
a = pkg.some_variable.field

# Here more or less the same applies, we are importing a single symbol and use it as a function. We should
# infer the function "pkg.function".
b = pkg.function()

# Here we are importing the whole "another_pkg" module and using a function from it. We should infer the existance
# of "another_pkg" as namespace and "another_pkg.function" as a function
c = another_pkg.function()

# This is just an alias of the above
d = alias.function()

# This is a bit tricky, we should be able to see because of the last import in line 4 that "pkg.third_module" is a module
# and infer a namespace "pkg.third_module". This should then be a static reference to a variable in that module
e = third_module.variable

f = function()

def foo(bar = pkg.some_variable):
    pass
