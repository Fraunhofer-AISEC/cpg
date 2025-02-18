def foo(arg):
    return 7

def list_comp(x, y):
    a = [foo(i) for i in x if i == 10]
    b = [foo(i) for i in x]
    c = {foo(i) for i in x if i == 10 if i < 20}
    d = [foo(i) for z in y if z in x for i in z if i == 10 ]
    foo(i)

def set_comp(x, y):
    a = {foo(i) for i in x if i == 10}
    b = {foo(i) for i in x}
    c = {foo(i) for i in x if i == 10 if i < 20}
    d = {foo(i) for z in y if z in x for i in z if i == 10 }

def dict_comp(x, y):
    a = {i: foo(i) for i in x if i == 10}
    b = {i: foo(i) for i in x}
    c = {i: foo(i) for i in x if i == 10 if i < 20}
    d = {i: foo(i) for z in y if z in x for i in z if i == 10 }

def generator(x, y):
    a = (i**2 for i in range(10) if i == 10)
    b = (i**2 for i in range(10))

def bar(k, v):
    return k+v

def tuple_comp(x):
    a = [bar(k, v) for (k, v) in x]

def comp_binding(foo):
    # As of Python 3, none of the comprehensions should bind to the outer x
    x = 42
    [x for x in foo]
    {x for x in foo}
    {x: x for x in foo}
    {x**2 for x in foo}
    print(x) # this prints 42

def comp_binding_assign_expr(foo):
    # https://peps.python.org/pep-0572/#scope-of-the-target
    x = 42
    [(x := temp) for temp in foo]
    print(x) # doesn't print 42

def comp_binding_assign_expr_nested(foo):
    # https://peps.python.org/pep-0572/#scope-of-the-target
    x = 42
    [[(x := temp) for temp in foo] for a in foo]
    print(x) # doesn't print 42

def comprehension_with_list_assignment():
    b = [0, 1, 2]
    [a for (a, b[0]) in [(1, 2), (2, 4), (3, 6)]]
    print(b) # prints [6, 1, 2]

def comprehension_with_list_assignment_and_index_variable():
    b = [0, 1, 2]
    [a for (a, b[a]) in [(0, 'this'), (1, 'is'), (2, 'fun')]]
    print(b) # prints ['this', 'is', 'fun']

def comprehension_with_list_assignment_and_index_variable_reversed():
    b = [0, 1, 2]
    a = 1
    [a for (b[a], a) in [('this', 0), ('is', 1), ('fun', 2)]] # This crashes because the "a" in the tuple shadows the outer variable and "UnboundLocalError: cannot access local variable 'a' where it is not associated with a value".
    print(b) # prints nothing due to crash

def comprehension_with_list_assignment_and_local_index_variable():
    b = [0, 1, 2]
    c = 1
    [a for (b[c], a) in [('this', 0), ('is', 1), ('fun', 2)]]
    print(b) # prints [0, 'fun', 2]

def list_comprehension_to_list_index():
    b = [0, 1, 2]
    [b[0] for b[0] in ['this', 'is', 'fun']]
    print(b) # prints ['fun', 1, 2]

class Magic:
    a = 7

def list_comprehension_to_field():
    b = Magic()
    [b.a for b.a in ['this', 'is', 'fun']]
    print(b.a) # prints 'fun'
