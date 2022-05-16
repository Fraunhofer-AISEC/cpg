import os

allfiles= os.listdir('./')

print("  & functions & longest function & \# variables & max variables in a function & global vars")

for filename in allfiles:
    max_counter = 0
    counter = 0
    in_function = False
    variables = 0
    variables_func = 0
    max_variables_func = 0
    global_vars = 0
    functions = 0
    with open(filename) as f:
        for l in f:
            counter += 1
            if l.strip().startswith("%") or l.strip().startswith("@"):
                if l.strip().startswith("@"):
                    global_vars += 1
                variables += 1
                variables_func += 1
            if "define" in l and "{" in l:
                counter = 0
                functions += 1
                variables_func = 0
                in_function = True
                args = min(l.count('%'), l.count(',')+1)
                variables += args
                variables_func += args

            if in_function and "}" in l:
                if max_counter < counter:
                    max_counter = counter
                if variables_func > max_variables_func:
                    max_variables_func = variables_func
                counter = 0
                in_function = False
    print(filename.split("-")[0] + " & " + str(functions) + " & " + str(max_counter) + " & " + str(variables) + " & " + str(max_variables_func) + " & " + str(global_vars))
