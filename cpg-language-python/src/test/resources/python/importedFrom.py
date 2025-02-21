import foo
import foo as foo_alias
import os as not_os
from os import times as t

foo.bar() # This should be mapped to the `import foo` import
foo_alias.bar() # This should be mapped to the `import foo as foo_alias`
os.times() # This should not be mapped to the `import os as not_os`
t() # This should be mapped to the `from os import times as t`