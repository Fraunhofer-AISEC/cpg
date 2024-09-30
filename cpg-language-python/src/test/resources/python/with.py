with open("file.txt", "r") as file:
    data = file.read()

with open('file1.txt') as f1, open('file2.txt') as f2:
    pass

class MyCustomType:
    pass

with MyCustomType() as my_type: #type: MyCustomType
    pass