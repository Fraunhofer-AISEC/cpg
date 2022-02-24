#
# Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#                    $$$$$$\  $$$$$$$\   $$$$$$\
#                   $$  __$$\ $$  __$$\ $$  __$$\
#                   $$ /  \__|$$ |  $$ |$$ /  \__|
#                   $$ |      $$$$$$$  |$$ |$$$$\
#                   $$ |      $$  ____/ $$ |\_$$ |
#                   $$ |  $$\ $$ |      $$ |  $$ |
#                   \$$$$$   |$$ |      \$$$$$   |
#                    \______/ \__|       \______/
#
from ._spotless_dummy import *


class CodeExtractor:
    # Simple/ugly class to extrace code snippets given a region
    def __init__(self, fname):
        with open(fname) as f:
            self.lines = f.read().splitlines()

    def get_snippet(self, lineno, col_offset, end_lineno, end_col_offset):
        # 1 vs 0-based indexing
        lineno -= 1
        # col_offset -= 1
        end_lineno -= 1
        # end_col_offset -= 1
        if lineno == end_lineno:
            return self.lines[lineno][col_offset:end_col_offset]
        else:
            res = []
            # first line is partially read
            res.append(" " * col_offset + self.lines[lineno][col_offset:])
            lineno += 1

            # fill with compelte lines
            while lineno < end_lineno:
                res.append(self.lines[lineno][:])
                lineno += 1

            # last line is partially read
            res.append(self.lines[end_lineno][:end_col_offset])

            return "\n".join(res)
