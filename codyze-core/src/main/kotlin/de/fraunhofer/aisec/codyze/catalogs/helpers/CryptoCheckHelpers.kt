/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.codyze.catalogs.helpers

import de.fraunhofer.aisec.codyze.catalogs.CryptoCatalog
import de.fraunhofer.aisec.cpg.assumptions.AssumptionType
import de.fraunhofer.aisec.cpg.assumptions.assume
import de.fraunhofer.aisec.cpg.graph.Backward
import de.fraunhofer.aisec.cpg.graph.ContextSensitive
import de.fraunhofer.aisec.cpg.graph.FieldSensitive
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.Interprocedural
import de.fraunhofer.aisec.cpg.graph.concepts.manualExtensions.*
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.Cipher
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.HybridCipher
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.SymmetricCipher
import de.fraunhofer.aisec.cpg.query.GenericQueryOperators
import de.fraunhofer.aisec.cpg.query.Must
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.query.dataFlow

fun SymmetricCipher.isAES(): QueryTree<Boolean> {
    val algoAccepted = this.cipherName?.uppercase()?.contains("AES") == true
    val algoCheck =
        QueryTree(
            value = algoAccepted,
            stringRepresentation =
                if (algoAccepted) {
                    "Algorithm is AES"
                } else {
                    "Algorithm $cipherName is not AES"
                },
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )
    return algoCheck
}

fun SymmetricCipher.isModus(modus: String): QueryTree<Boolean> {
    val modusAccepted = this.cipherName?.uppercase()?.contains(modus) == true
    val modusCheck =
        QueryTree(
            value = modusAccepted,
            stringRepresentation =
                if (modusAccepted) {
                    "Modus is $modus"
                } else {
                    "Modus $cipherName is not $modus"
                },
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )
    return modusCheck
}

fun SymmetricCipher.keyIsBlockSize(): QueryTree<Boolean> {
    val keySizeIsBlockSize = this.keySize == this.blockSize
    val keyIsBlockSizeCheck =
        QueryTree(
            value = keySizeIsBlockSize,
            stringRepresentation =
                if (keySizeIsBlockSize) {
                    "Key size matches block size"
                } else {
                    "Key size $keySize does not match block size $blockSize"
                },
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )
    return keyIsBlockSizeCheck
}

fun SymmetricCipher.validKeyLength(keySizes: Collection<Int>): QueryTree<Boolean> {
    val validKeyLength = this.keySize in keySizes
    val validKeyLengthCheck =
        QueryTree(
            value = validKeyLength,
            stringRepresentation =
                if (validKeyLength) {
                    "Key size is in the list of allowed values: $keySizes"
                } else {
                    "Key size $keySize is not in the list of allowed values: $keySizes"
                },
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )
    return validKeyLengthCheck
}

fun SymmetricCipher.validAuthTagSize(minimalAuthTagSize: Int): QueryTree<Boolean> {
    val tagSizeAccepted =
        (this.authTagSize
            ?: return QueryTree(
                value = false,
                stringRepresentation = "Unknown tag size for cipher",
                node = this,
                operator = GenericQueryOperators.EVALUATE,
            )) >= minimalAuthTagSize
    val tagSizeCheck =
        QueryTree(
            value = tagSizeAccepted,
            stringRepresentation =
                if (tagSizeAccepted) {
                    "The authentication tag length ${this.authTagSize} is at least $minimalAuthTagSize bits"
                } else {
                    "The authentication tag length ${this.authTagSize} is smaller than $minimalAuthTagSize bits"
                },
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )
    return tagSizeCheck
}

fun SymmetricCipher.validIvSize(minimalIvSize: Int): QueryTree<Boolean> {
    val ivSizeAccepted =
        (this.initializationVector?.size
            ?: return QueryTree(
                value = false,
                stringRepresentation = "Unknown IV size for cipher",
                node = this,
                operator = GenericQueryOperators.EVALUATE,
            )) >= minimalIvSize
    val ivSizeCheck =
        QueryTree(
            value = ivSizeAccepted,
            stringRepresentation =
                if (ivSizeAccepted) {
                    "The IV length ${this.initializationVector?.size} is at least $minimalIvSize bits"
                } else {
                    "The IV length ${this.initializationVector?.size} is smaller than $minimalIvSize bits"
                },
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )
    return ivSizeCheck
}

fun SymmetricCipher.ivSizeEqualsBlockSize(): QueryTree<Boolean> {
    val ivSizeAccepted = this.initializationVector?.size == blockSize
    val ivSizeCheck =
        QueryTree(
            value = ivSizeAccepted,
            stringRepresentation =
                if (ivSizeAccepted) {
                    "The IV length ${this.initializationVector?.size} is the same as the block size $blockSize bits"
                } else {
                    "The IV length ${this.initializationVector?.size} is different than the block size $blockSize bits"
                },
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )
    return ivSizeCheck
}

fun SymmetricCipher.isUniqueIv(): QueryTree<Boolean> {
    // TODO: We cannot check this statically, but it is a requirement that the IV is unique for each
    // encryption operation with the same key.
    val ivIsUnique = true
    val ivIsUniqueCheck =
        QueryTree(
                value = ivIsUnique,
                stringRepresentation =
                    if (ivIsUnique)
                        "The IV is unique for each encryption operation with the same key"
                    else "The IV is not unique for each encryption operation with the same key",
                node = this,
                operator = GenericQueryOperators.EVALUATE,
            )
            .assume(
                AssumptionType.ConceptAssumption,
                "We cannot check the uniqueness of the IV statically, so we assume it is unique.\n\nThis should be verified manually.",
            )
    return ivIsUniqueCheck
}

fun SymmetricCipher.isRandomIv(): QueryTree<Boolean> {
    val iv =
        this.initializationVector
            ?: return QueryTree(
                value = false,
                stringRepresentation = "No IV is set for the cipher but required",
                node = this,
                operator = GenericQueryOperators.EVALUATE,
            )

    return dataFlow(
        iv,
        direction = Backward(GraphToFollow.DFG),
        type = Must,
        sensitivities = FieldSensitive + ContextSensitive,
        scope = Interprocedural(),
        predicate = { it is RngGet },
    )
}

fun Cipher.keySizeBiggerThan(minimalSize: Int): QueryTree<Boolean> =
    this.keySize?.let {
        QueryTree(
            value = it >= minimalSize,
            stringRepresentation =
                if (it >= minimalSize) "The keysize is bigger than $minimalSize bit"
                else "The keysize $it is smaller than the required $minimalSize bit",
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )
    }
        ?: QueryTree(
            value = false,
            stringRepresentation = "Could not identify the keysize",
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )

fun Cipher.isRSA(): QueryTree<Boolean> =
    QueryTree(
        value = this.cipherName == "RSA",
        stringRepresentation =
            if (cipherName == "RSA") "The algorithm is RSA" else "The algorithm is not RSA",
        node = this,
        operator = GenericQueryOperators.EVALUATE,
    )

/**
 * Checks if the key exchange mechanism used by this [HybridCipher] is considered state of the art
 * according to the provided [de.fraunhofer.aisec.confirmate.queries.catalogs.CryptoCatalog].
 */
context(catalog: CryptoCatalog)
fun HybridCipher.isKeyExchangeOk() =
    this.keyExchange?.let { keyExchange -> with(keyExchange) { catalog.checkKeyExchange() } }
        ?: QueryTree(
            value = false,
            stringRepresentation =
                "Could not identify the key exchange mechanism of the hybrid algorithm",
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )

/**
 * Checks if the symmetric cipher used by this [HybridCipher] is considered state of the art
 * according to the provided [CryptoCatalog].
 */
context(catalog: CryptoCatalog)
fun HybridCipher.isSymmetricCipherOk() =
    this.symmetricCipher?.let { symmetricCipher ->
        with(symmetricCipher) { catalog.checkSymmetricEncryption() }
    }
        ?: QueryTree(
            value = false,
            stringRepresentation =
                "Could not identify the symmetric cipher with the hybrid algorithm",
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )

/**
 * Checks if the hash function used by this [HybridCipher] is considered state of the art according
 * to the provided [CryptoCatalog].
 */
context(catalog: CryptoCatalog)
fun HybridCipher.isHashFunctionOk() =
    this.keyDerivationFunction?.let { kdf -> with(kdf) { catalog.checkKDF() } }
        ?: QueryTree(
            value = false,
            stringRepresentation = "Could not identify the hash function of the hybrid algorithm",
            node = this,
            operator = GenericQueryOperators.EVALUATE,
        )
