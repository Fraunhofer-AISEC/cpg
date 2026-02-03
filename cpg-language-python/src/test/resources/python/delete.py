a = 5
b = a
del a

my_list = [1, 2, 3]
del my_list[2]

my_dict = {'a': 1, 'b': 2}
del my_dict['b']

class MyClass:
    def __init__(self):
        self.d = 1

obj = MyClass()
del obj.d