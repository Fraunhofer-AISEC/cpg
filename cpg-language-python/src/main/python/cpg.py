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

def parse_comments(code, tud, frontend):
        reader = io.StringIO(code).read
        comment_tokens = (t for t in tokenize.generate_tokens(reader) if t.type == tokenize.COMMENT or t.type == tokenize.NL or t.type == tokenize.NEWLINE)
        comment = None
        nr_newlines = 0
        nl_position = -1
        cmt_position = 0
        for token in comment_tokens:
            # comment = next(comment_tokens, None)
            if token.type == tokenize.COMMENT:
                comment = code[token.start[1]:]
                cmt_position = token.start[1]
            if token.type == tokenize.NL or token.type == tokenize.NEWLINE:
                if comment:
                    comment = comment[:token.start[1] - (len(code) - len(comment))]
                    frontend.matchCommentToNode(comment, Region(nr_newlines + 1,cmt_position - nl_position,nr_newlines + 1,token.start[1]-nl_position), tud)
                    comment = None

                nr_newlines += 1
                nl_position = token.start[1]
