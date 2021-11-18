package de.fraunhofer.aisec.cpg.helpers.annotations

/**
 * This annotation is used to mark functions which translate instructions/statements
 * to special functions which do not actually exist in the respective language.
 *
 * This eases an identification of such additional functions
 */
annotation class FunctionReplacement(val addedFunctions: Array<String>, val replaces: String = "")
