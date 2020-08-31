package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import java.util.*

/**
 * A declaration chain is a double-linked list that links together a list of declarations.
 */
class DeclarationChain<N : Declaration> {
    /**
     * The first declaration we have seen.
     */
    var first: ReDeclarable<N>? = null

    /**
     * The previous declaration, seen from the current one
     */
    var prev: ReDeclarable<N>? = null

    /**
     * The latest declaration we have seen.
     */
    var latest: ReDeclarable<N>? = null
}

/**
 * This interface represents a node which can be re-declared.
 */
interface ReDeclarable<N : Declaration> : Iterable<N> {

    abstract val nextDeclaration: N?

    abstract val chain: DeclarationChain<N>

    @JvmDefault
    override operator fun iterator(): Iterator<N> {
        return declarationIterator(this);
    }

    @JvmDefault
    fun declarationIterator(node: ReDeclarable<*>): Iterator<N> {
        return object : Iterator<N> {
            var current = node
            var passedFirst = false
            override fun hasNext(): Boolean {
                if (passedFirst && current === chain.first) {
                    return false
                }
                val next = peek()
                return next !== current
            }

            private fun peek(): N {
                return current.nextDeclaration as N
            }

            override fun next(): N {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                val next = peek()
                current = next as ReDeclarable<*>
                if (current === chain.first) {
                    passedFirst = true
                }
                return next
            }
        }
    }

    /**
     * Sets the previous declaration of this, if it exists. If it does not exist, this is probably the
     * first.
     */
    @JvmDefault
    fun setPreviousDeclaration(previousDeclaration: ReDeclarable<N>?) {
        if (previousDeclaration != null) {
            // learn first from previous
            chain.first = previousDeclaration.chain.first
            assert(chain.first != null)
            chain.prev = previousDeclaration
        } else {
            chain.first = this
        }

        // point first one's latest to us
        val first = chain.first
        first?.chain?.latest = this

        updateFields()
    }

    @JvmDefault
    val previous: ReDeclarable<N>?
        get() = // we do not know the latest, but if we know the previous
            if (chain.latest == null && chain.prev != null) {
                // return it
                chain.prev
            } else {
                // otherwise, return the latest
                chain.latest
            }

    @JvmDefault
    val mostRecentDeclaration: N
        get() = (chain.latest ?: this) as N

    fun updateFields() {}
    fun isSameDeclaration(declaration: N): Boolean
}