# Python Support for the CPG

## Code Style
Code should pass the `pycodestyle` check.

You could use `autopep8 --in-place --recursive --aggressive --aggressive .` for auto formatting, but be careful as this can be too aggressive and produce weird results.

## Debugging Python Code
Debugging the Python code can be a bit tricky, as it is executed via JEP. A solution is to attach a remote debugger to the Python process. The following guide details how to achieve this with an ultimate version of Intellij (the free version does not support remote debugging).

There is currently support for the "pydevd_pycharm" debugger implemented. Other remote debuggers can probably be attached in a similar fashion. To enable the debugging functionality, set the `DEBUG_PYTHON_EGG` environment variable for your run configuration (e.g. `DEBUG_PYTHON_EGG=/home/user/.local/share/JetBrains/IntelliJIdea2022.3/python/debugger-eggs-output/pydevd-pycharm.egg`). You can also set the host and port via `DEBUG_PYTHON_HOST` and `DEBUG_PYTHON_PORT`, respectively. Otherwise, those will default to `localhost` and `52190`.
Alternatively, you can install (and keep it up-to-date with the Intellij version) `pydevd_pycharm` in your Python environment and modify the files `PythonLanguageFrontend.kt` and `cpg.py` accordingly.

Now, add a "Run/Debug Configuration" in Intellij and choose the "Python Debug Server". Configure the host name and port according to the values set above.

You should also set a proper "Path mapping" according to your local configuration. For example:

`/home/user/git/cpg/cpg-language-python/src/main/python` as "Local path" -> `/home/user/git/cpg/cpg-language-python/build/resources/main` as "Remote path"

Finally, you are all set to debug Python code:
1. Start the Python Debug Server (run configuration prepared above).
2. Run your code `PythonLanguageFrontendTests`. This will trigger the Python debugging and you can use all the debugger features :)