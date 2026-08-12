if foo():
    file = open('/tmp/example.txt', 'w')
else:
    file = open('/notTmp/example.txt', 'w')
file.write('Hello world!')