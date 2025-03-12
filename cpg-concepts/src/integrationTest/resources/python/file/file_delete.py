import os

with open('example.txt', 'r') as file:
    content = file.read()

os.remove('example.txt')

with open('example.txt', 'w') as file:
    file.write(content)
