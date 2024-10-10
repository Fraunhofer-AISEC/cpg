# module foobar.implementation exports "foo" from foobar.implementation.internal_foo,
# so we can directly import it
from foobar.implementation import foo

# bar is not exported in the package, so we need to import the specific module to
# access it
from foobar.implementation.internal_bar import bar

bar()
foo()

print("Main!")
