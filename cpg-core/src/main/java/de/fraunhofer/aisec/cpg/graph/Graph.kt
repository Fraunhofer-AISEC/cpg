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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.ExperimentalGraph
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.TimeBenchmark
import java.io.Closeable
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.time.ExperimentalTime
import org.neo4j.ogm.annotation.Relationship
import org.opencypher.v9_0.ast.*
import org.opencypher.v9_0.expressions.*
import org.opencypher.v9_0.parser.CypherParser
import org.slf4j.LoggerFactory
import scala.Option

@ExperimentalGraph
class QueryBenchmark constructor(db: Graph, query: Query) :
    TimeBenchmark(db.javaClass, "totalNodes: " + db.size() + " query: " + query.toString()),
    AutoCloseable,
    Closeable {
    override fun close() {
        addMeasurement()
    }
}

/** Returns a {@link Graph} object starting from this translation result. */
@ExperimentalGraph
val TranslationResult.graph: Graph
    get() {
        // TODO: retrieve ALL nodes, not just AST, i.e. we are missing type nodes
        val nodes = SubgraphWalker.flattenAST(this)

        return Graph(nodes)
    }

/**
 * A query context represents an interface between an openCypher query and the actual graph. It
 * contains of different handler functions, belonging to different language constructs in the
 * openCypher language, such as MATCH or RETURN. These handler functions basically execute the query
 * on the graph.
 *
 * Please see https://github.com/Fraunhofer-AISEC/cpg/pull/276 for an overview of currently
 * supported openCypher features.
 */
@ExperimentalGraph
class QueryContext constructor(val graph: Graph) {
    val results = mutableMapOf<Variable, MutableList<Node>>()

    internal fun handleReturn(`return`: Return): List<Node> {
        // hacky hack
        val item = (`return`.returnItems() as ReturnItems).items().head() as UnaliasedReturnItem
        val variable = item.expression() as Variable

        val nodes = results[variable]
        if (nodes != null) {
            val limit: Limit? = `return`.limit().getOrElse { null }
            if (limit != null) {
                val expression = limit.expression()

                if (expression is IntegerLiteral) {
                    return nodes.stream().limit(expression.value()).collect(Collectors.toList())
                } else {
                    throw RuntimeException("Only non-negative integers are allowed")
                }
            }

            return nodes
        }

        return emptyList()
    }

    internal fun handleMatch(match: Match) {
        // find out which class we need (only first pattern for now)
        val pattern = match.pattern().patternParts().head()
        if (pattern is EveryPath) {
            handleEveryPath(pattern, match.where())
        }
    }

    private fun handleEveryPath(pattern: PatternPart, where: Option<Where>) {
        val element = pattern.element()

        if (element is NodePattern) {
            handleNodePattern(element, graph.nodes, where, null)
        } else if (element is RelationshipChain) {
            handleRelationshipChain(element, graph.nodes, where)
        }
    }

    private fun handleRelationshipChain(
        chain: RelationshipChain,
        nodes: List<Node>,
        where: Option<Where>
    ) {
        // relationship = we need the label for now
        val relationship = chain.relationship()

        if (relationship.direction() is SemanticDirection.`BOTH$`) {
            TODO("Not supporting relationships with both directions yet")
        }

        if (relationship.types().isEmpty) {
            TODO("Not supporting relationships without type yet")
        } else {
            val type = relationship.types().head()

            // creating a predicate that checks for the existence of a relationship
            val predicate: (Node) -> Boolean = {
                var relationshipProperty =
                    it[
                        type.name().lowercase(Locale.getDefault()),
                        relationship.direction().toString()]

                // check for the existence of the edge
                if (relationshipProperty is Collection<*>) {
                    // TODO: check if it really needs unwrapping, not all our nodes are modelled
                    // this way
                    relationshipProperty =
                        PropertyEdge.unwrap(relationshipProperty as MutableList<PropertyEdge<Node>>)

                    if (chain.rightNode() is NodePattern) {
                        val list =
                            handleNodePattern(chain.rightNode(), relationshipProperty, where, null)
                        list.isNotEmpty()
                    } else {
                        TODO()
                    }
                } else {
                    relationshipProperty != null
                }
            }

            // finally handle it like a normal node
            handleNodePattern(chain.element() as NodePattern, nodes, where, predicate)
        }
    }

    private fun handleNodePattern(
        element: NodePattern,
        nodes: List<Node>,
        where: Option<Where>,
        predicate: Predicate<in Node>?
    ): List<Node> {
        var stream = nodes.parallelStream()

        val labels = element.labels()

        // only one label for now
        if (!labels.isEmpty) {
            val label = labels.head()

            stream = stream.filter { it.labels.contains(label.name()) }
        }

        // variable seems optional
        val o = element.variable()
        val variable: Variable? = o.getOrElse { null }

        // lets do the where
        if (where.isDefined) {
            val inner = where.get()

            val expression = inner.expression()
            // check for variables
            val list = getVariables(expression)

            if (list.size > 1) {
                TODO("Cannot handle comparison between two variables yet")
            }

            /*list.forEach {
                if (!variables.contains(it)) {
                    throw RuntimeException("Variable ${it.name()} is not defined")
                }
            }*/

            // only select it, if contains a variable bound for this node pattern; or no variables
            // are present
            if (list.isEmpty() || list.contains(variable)) {
                stream =
                    when (expression) {
                        is Equals -> handleEquals(expression, stream)
                        is LessThan -> handleLessThan(expression, stream)
                        is GreaterThan -> handleGreaterThan(expression, stream)
                        else -> {
                            TODO()
                        }
                    }
            }
        }

        if (predicate != null) {
            stream = stream.filter(predicate)
        }

        val list = stream.collect(Collectors.toList())

        // we could do a little optimization and just return a stream and let the caller handle the
        // collection, this would be an optimization
        // if no variable is used and thus just the existence is needed, i.e. for filter, but we do
        // not need the result
        variable?.let {
            // collect the results and put it into the variable list
            if (!results.containsKey(it)) {
                results[it] = list
            } else {
                results[it]?.addAll(list)
            }
        }

        return list
    }

    private fun handleEquals(expression: Equals, stream: Stream<Node>): Stream<Node> {
        return stream.filter {
            val lhs = handleExpression(it, expression.lhs())
            val rhs = handleExpression(it, expression.rhs())

            if (lhs is Number && rhs is Number) {
                lhs.toLong() == rhs.toLong()
            } else {
                lhs == rhs
            }
        }
    }

    private fun handleLessThan(expression: LessThan, stream: Stream<Node>): Stream<Node> {
        return stream.filter {
            val lhs = handleExpression(it, expression.lhs())
            val rhs = handleExpression(it, expression.rhs())

            if (lhs is Number && rhs is Number) {
                lhs.toLong() < rhs.toLong()
            } else if (lhs is String && rhs is String) {
                lhs < rhs
            } else {
                throw java.lang.RuntimeException("Cannot compare")
            }
        }
    }

    private fun handleGreaterThan(expression: GreaterThan, stream: Stream<Node>): Stream<Node> {
        return stream.filter {
            val lhs = handleExpression(it, expression.lhs())
            val rhs = handleExpression(it, expression.rhs())

            if (lhs is Number && rhs is Number) {
                lhs.toLong() > rhs.toLong()
            } else if (lhs is String && rhs is String) {
                lhs > rhs
            } else {
                throw java.lang.RuntimeException("Cannot compare")
            }
        }
    }

    private fun handleExpression(node: Node, expression: Expression): Any? {
        if (expression is Literal) {
            return handleLiteral(expression)
        } else if (expression is Property) {
            return handleProperty(node, expression)
        }

        return null
    }

    private fun handleProperty(node: Node, property: Property): Any? {
        // it seems, that non-valid properties are silently ignored, need to check
        // that in the openCypher specification
        return node[property.propertyKey().name()]
    }

    private fun handleLiteral(literal: Literal): Any {
        return literal.value()
    }

    private fun getVariables(expression: Expression): List<Variable> {
        if (expression is Property) {
            return getVariables(expression)
        } else if (expression is Equals) {
            return getVariables(expression)
        }

        return emptyList()
    }

    private fun getVariables(equals: Equals): List<Variable> {
        val list = mutableListOf<Variable>()

        list.addAll(getVariables(equals.lhs()))
        list.addAll(getVariables(equals.rhs()))

        return list
    }

    private fun getVariables(property: Property): List<Variable> {
        return listOf(property.map() as Variable)
    }
}

/**
 * Represents a graph consisting of the specified nodes. It can be queried using openCypher queries.
 */
@ExperimentalGraph
class Graph(var nodes: List<Node>) {

    private val logger = LoggerFactory.getLogger(Graph::class.java)

    private val parser = CypherParser()

    fun size(): Int {
        return nodes.size
    }

    operator fun plusAssign(node: Node) {
        nodes += node
    }

    fun query(queryText: String): List<Node> {
        val query = parser.parse(queryText, null, null) as Query

        return executeQuery(query)
    }

    @OptIn(ExperimentalTime::class)
    fun executeQuery(query: Query): List<Node> {
        val ctx = QueryContext(this)

        var list: List<Node> = listOf()
        val b = QueryBenchmark(this, query)
        b.use {
            // very hacky for now
            val singleQuery = query.part() as SingleQuery

            for (clause in singleQuery.clauses()) {
                // TODO: have a map of streams for the variables?
                if (clause is Match) {
                    ctx.handleMatch(clause)
                } else if (clause is Return) {
                    list = ctx.handleReturn(clause)
                }
            }
        }

        logger.info("Query took ${b.measurements.entries.firstOrNull()?.value}")

        return list
    }
}

/**
 * Returns the node's label based on its class hierarchy. It might seem like a good idea to cache
 * this somehow but for some reason, it is faster to do it this way. An example query run almost
 * twice as fast this way. So it looks like Kotlin optimizes the heck out of this.
 */
val Node.labels: List<String>
    get() {
        val labels = mutableListOf<String>()

        var clazz: Class<*>? = this.javaClass
        while (clazz != null && clazz != Object::class.java) {
            labels += clazz.simpleName
            clazz = clazz.superclass
        }

        return labels
    }

operator fun Node.get(key: String): Any? {
    return getProperty(this.javaClass, key)
}

operator fun Node.get(key: String, direction: String): Any? {
    return getRelationship(this.javaClass, key, direction)
}

private fun Node.getRelationship(clazz: Class<*>, key: String, direction: String): Any? {
    return try {
        val field = clazz.getDeclaredField(key)

        if (field.trySetAccessible()) {
            val annotation = field.getAnnotation(Relationship::class.java)

            if (annotation.direction == direction) {
                field.get(this)
            } else {
                null
            }
        } else {
            // TODO: throw exception?
            null
        }
    } catch (e: NoSuchFieldException) {
        if (clazz.superclass != null) {
            getRelationship(clazz.superclass, key, direction)
        } else {
            null
        }
    }
}

private fun Node.getProperty(clazz: Class<*>, key: String): Any? {
    // TODO: cache fields somehow?
    return try {
        val field = clazz.getDeclaredField(key)

        if (field.trySetAccessible()) {
            field.get(this)
        } else {
            // TODO: throw exception?
            null
        }
    } catch (e: NoSuchFieldException) {
        if (clazz.superclass != null) {
            getProperty(clazz.superclass, key)
        } else {
            null
        }
    }
}
