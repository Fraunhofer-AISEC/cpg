from pkg import some_variable, function
import another_pkg
import another_pkg as alias

a = pkg.some_variable.field
b = pkg.function()
c = another_pkg.function()
d = alias.function()