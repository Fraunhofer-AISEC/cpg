### CPG Python

This module is essential for setting up the Python frontend for CPG. 
Python parsing is accomplished using native Python, necessitating the installation of [JEP](https://github.com/ninia/jep) for proper JNI functionality. Follow the instructions below to install JEP and enable the frontend.

### Installation

1. This module is disabled by default, so you need to enable it by adding the following to the root `gradle.properties`:

    ```plaintext
    enablePythonFrontend=true
    ```

2. If you have an x86 architecture, no further action is required as JEP will be automatically installed for Python 3.9-3.11 
from [https://github.com/icemachined/jep-distro/releases/](https://github.com/icemachined/jep-distro/releases/).
Check for the latest supported version of Python or JEP on that page. Python version can be set with `-Ppython` property:
`./gradlew cpg-language-python:build -Ppython=3.12`, the default is `3.10`

If you have a different architecture or wish to use another version of JEP or Python, you will need to install it manually. Here are potential solutions:
- Utilize Homebrew/pip package manager to simplify the JEP installation process:

    ```plaintext
    brew install pip3
    pip3 install jep
    ```
  
- Create a [virtual environment](https://docs.python.org/3/library/venv.html) with the specified environment variable `CPG_PYTHON_VIRTUALENV` 
set to `/(user.home)/.virtualenv/CPG_PYTHON_VIRTUALENV`.
- Manually install JEP and specify the `CPG_JEP_LIBRARY` environment variable with the appropriate path to the installation.

3. `./gradlew cpg-language-python:build`