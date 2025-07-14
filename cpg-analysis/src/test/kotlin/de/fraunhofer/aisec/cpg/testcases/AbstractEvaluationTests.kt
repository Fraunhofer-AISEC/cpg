/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.testcases

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.passes.UnreachableEOGPass

abstract class AbstractEvaluationTests {
    companion object {
        /*
        public class IntegerTest {
            public void f1() {
                Bar b = new Bar();
                int a = 5;

                a = 0;
                a -= 2;
                a += 3;

                b.f(a);
            }

            public void f2() {
               Bar f = new Bar();
               int a = 5;

               a = 3;
               a++;
               ++a;
               a -= 2;
               a += 3;
               a--;
               --a;
               a *= 4;
               a /= 2;
               a %= 3;

               b.f(a);
            }

            public void f3() {
                Bar b = new Bar();
                int a = 5;

                if (new Random().nextBoolean()) {
                    a -= 1;
                }

                b.f(a);
            }

            public void f4() {
                Bar b = new Bar();
                int a = 5;

                if (new Random().nextBoolean()) {
                    a -= 1;
                } else {
                    a = 3;
                }

                b.f(a);
            }

            public void f5() {
                Bar b = new Bar();
                int a = 5;

                for (int i = 0; i < 5; i++) {
                    a += 1;
                }

                b.f(a);
            }
        }

        class Bar {
            public void f(int a) {}
        }
         */
        fun getIntegerExample(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .registerPass<UnreachableEOGPass>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("integer.java") {
                        record("Foo") {
                            method("f1") {
                                body {
                                    declare { variable("b", t("Bar")) }
                                    declare { variable("a", t("int")) { literal(5, t("int")) } }

                                    ref("a") assign literal(0, t("int"))
                                    ref("a") assignMinus literal(2, t("int"))
                                    ref("a") assignPlus literal(3, t("int"))

                                    memberCall("f", ref("Bar")) { ref("a") }
                                }
                            }
                            method("f2") {
                                body {
                                    declare { variable("b", t("Bar")) }
                                    declare { variable("a", t("int")) { literal(5, t("int")) } }

                                    ref("a") assign literal(3, t("int"))

                                    ref("a").inc()
                                    ref("a").incPrefix()

                                    memberCall("c", ref("Bar")) { ref("a") }

                                    ref("a") assignMinus literal(2, t("int"))
                                    ref("a") assignPlus literal(3, t("int"))

                                    ref("a").dec()
                                    ref("a").decPrefix()

                                    ref("a") assignMult literal(4, t("int"))
                                    ref("a") assignDiv literal(2, t("int"))
                                    ref("a") assignMod literal(3, t("int"))

                                    memberCall("f", ref("Bar")) { ref("a") }
                                }
                            }
                            method("f3") {
                                body {
                                    declare { variable("b", t("Bar")) }
                                    declare { variable("a", t("int")) { literal(5, t("int")) } }

                                    ifStmt {
                                        condition { memberCall("nextBoolean", ref("Random")) }
                                        thenStmt { ref("a") assignMinus literal(1, t("int")) }
                                    }

                                    memberCall("f", ref("Bar")) { ref("a") }
                                }
                            }
                            method("f4") {
                                body {
                                    declare { variable("b", t("Bar")) }
                                    declare { variable("a", t("int")) { literal(5, t("int")) } }

                                    ifStmt {
                                        condition { memberCall("nextBoolean", ref("Random")) }
                                        thenStmt { ref("a") assignMinus literal(1, t("int")) }
                                        elseStmt { ref("a") assign literal(3, t("int")) }
                                    }

                                    memberCall("f", ref("Bar")) { ref("a") }
                                }
                            }
                            method("f5") {
                                body {
                                    declare { variable("b", t("Bar")) }
                                    declare { variable("a", t("int")) { literal(5, t("int")) } }

                                    forStmt {
                                        forInitializer {
                                            declare {
                                                variable("i", t("int")) { literal(0, t("int")) }
                                            }
                                        }
                                        forCondition { ref("i") lt literal(5, t("int")) }
                                        forIteration { ref("i").inc() }

                                        loopBody {
                                            ref("a") assignPlus literal(1, t("int"))
                                            call("println") { ref("i") }
                                        }
                                    }

                                    memberCall("f", ref("Bar")) { ref("a") }
                                }
                            }
                        }
                        record("Bar") {
                            method("main") {
                                param("a", t("int"))
                                body {}
                            }
                        }
                    }
                }
            }
    }
}
