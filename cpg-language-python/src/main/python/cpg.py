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
from CPGPython import PythonASTToCPG
import io
import tokenize
from de.fraunhofer.aisec.cpg.sarif import Region


def parse_code(code, filename, frontend):
    try:
        converter = PythonASTToCPG(filename, frontend, code)
        converter.execute()

        tud = converter.tud

        parse_comments(code, tud, frontend)

        return tud
    except Exception as e:
        frontend.log.error("Buidling the CPG failed with exception: %s" % (e))
        raise e

def parse_comments(filename, code, tud, frontend):
        reader = tokenize.open(filename).readline
        comment_tokens = (t for t in tokenize.generate_tokens(reader) if t.type == tokenize.COMMENT)
        for token in comment_tokens:
            frontend.matchCommentToNode(token.string,
                Region(token.start[0],token.start[1] + 1,token.end[0],token.end[1] + 1),
                tud)
