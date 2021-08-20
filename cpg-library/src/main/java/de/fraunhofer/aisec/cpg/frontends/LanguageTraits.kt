/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

/**
 * A language trait is a feature or trait that is common to a group of programming languages.
 * Examples could be the support of pointers, support for templates or generics.
 *
 * Currently, this interface has no methods. However, in the future, this could be used to execute
 * language/frontend-specific code for the particular trait. This could help to fine-tune the
 * [de.fraunhofer.aisec.cpg.passes.CallResolver] for specific languages.
 */
interface LanguageTrait

/** A language trait, that specifies that this language has support for templates or generics. */
interface HasTemplates : LanguageTrait

/**
 * A language trait that specifies, that this language has support for default arguments, e.g. in
 * functions.
 */
interface HasDefaultArguments : LanguageTrait
