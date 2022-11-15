import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright (c) 2019-2021, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */

plugins {
    id("com.diffplug.spotless")
}

// state that JSON schema parser must run before compiling Kotlin
tasks.withType<KotlinCompile> {
    dependsOn("spotlessApply")
}
tasks.withType<JavaCompile> {
    dependsOn("spotlessApply")
}

val headerWithStars = """/*
 * Copyright (c) ${"$"}YEAR, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\  ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\   ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\
 *                   ${'$'}${'$'}  __${'$'}${'$'}\ ${'$'}${'$'}  __${'$'}${'$'}\ ${'$'}${'$'}  __${'$'}${'$'}\
 *                   ${'$'}${'$'} /  \__|${'$'}${'$'} |  ${'$'}${'$'} |${'$'}${'$'} /  \__|
 *                   ${'$'}${'$'} |      ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}  |${'$'}${'$'} |${'$'}${'$'}${'$'}${'$'}\
 *                   ${'$'}${'$'} |      ${'$'}${'$'}  ____/ ${'$'}${'$'} |\_${'$'}${'$'} |
 *                   ${'$'}${'$'} |  ${'$'}${'$'}\ ${'$'}${'$'} |      ${'$'}${'$'} |  ${'$'}${'$'} |
 *                   \${'$'}${'$'}${'$'}${'$'}${'$'}   |${'$'}${'$'} |      \${'$'}${'$'}${'$'}${'$'}${'$'}   |
 *                    \______/ \__|       \______/
 *
 */
"""

val headerWithHashes = """#
# Copyright (c) ${"$"}YEAR, Fraunhofer AISEC. All rights reserved.
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
#                    ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\  ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\   ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\
#                   ${'$'}${'$'}  __${'$'}${'$'}\ ${'$'}${'$'}  __${'$'}${'$'}\ ${'$'}${'$'}  __${'$'}${'$'}\
#                   ${'$'}${'$'} /  \__|${'$'}${'$'} |  ${'$'}${'$'} |${'$'}${'$'} /  \__|
#                   ${'$'}${'$'} |      ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}  |${'$'}${'$'} |${'$'}${'$'}${'$'}${'$'}\
#                   ${'$'}${'$'} |      ${'$'}${'$'}  ____/ ${'$'}${'$'} |\_${'$'}${'$'} |
#                   ${'$'}${'$'} |  ${'$'}${'$'}\ ${'$'}${'$'} |      ${'$'}${'$'} |  ${'$'}${'$'} |
#                   \${'$'}${'$'}${'$'}${'$'}${'$'}   |${'$'}${'$'} |      \${'$'}${'$'}${'$'}${'$'}${'$'}   |
#                    \______/ \__|       \______/
#
"""

spotless {
    java {
        targetExclude(
            fileTree(project.projectDir) {
                include("build/generated-src/**")
            }
        )
        googleJavaFormat("1.15.0")
        licenseHeader(headerWithStars).yearSeparator(" - ")
    }

    kotlin {
        ktfmt().kotlinlangStyle()
        licenseHeader(headerWithStars).yearSeparator(" - ")
    }

    python {
        target("src/main/**/*.py")
        licenseHeader(headerWithHashes, "from").yearSeparator(" - ")
    }

    format("golang") {
        target("src/main/golang/**/*.go")
        licenseHeader(headerWithStars, "package").yearSeparator(" - ")
    }
}