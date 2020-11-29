package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.ExperimentalGraph
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import org.neo4j.ogm.annotation.Relationship
import org.opencypher.v9_0.ast.*
import org.opencypher.v9_0.expressions.*
import org.opencypher.v9_0.parser.CypherParser
import scala.Option
import java.io.Closeable
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@ExperimentalGraph
class QueryBenchmark constructor(db: Graph, query: Query) :
        Benchmark(db.javaClass,
                "totalNodes: " + db.size() + " query: " + query.toString()), AutoCloseable, Closeable {
    override fun close() {
        stop()
    }
}

/**
 * Returns a {@link Graph} object starting from this translation result.
 */
@ExperimentalGraph
val TranslationResult.graph: Graph
    get() {
        // TODO: retrieve ALL nodes, not just AST, i.e. we are missing type nodes
        val nodes = SubgraphWalker.flattenAST(this)

        return Graph(nodes)
    }


@ExperimentalGraph
class QueryContext constructor(val graph: Graph) {
    val streams = mutableMapOf<Variable, Stream<Node>>()

    internal fun handleReturn(`return`: Return): List<Node> {
        // hacky hack
        val item = (`return`.returnItems() as ReturnItems).items().head() as UnaliasedReturnItem
        val variable = item.expression() as Variable

        var stream = streams[variable]
        if (stream != null) {
            val limit: Limit? = `return`.limit().getOrElse { null }
            if (limit != null) {
                val expression = limit.expression()

                if (expression is IntegerLiteral) {
                    stream = stream.limit(expression.value())
                } else {
                    throw RuntimeException("Only non-negative integers are allowed")
                }
            }

            if (stream != null) {
                return stream.collect(Collectors.toList())
            }
        }

        return emptyList()
    }

    internal fun handleMatch(match: Match) {
        // keep track of bound variables
        val variables = mutableSetOf<LogicalVariable>()

        // find out which class we need (only first pattern for now)
        val pattern = match.pattern().patternParts().head()
        if (pattern is EveryPath) {
            handleEveryPath(pattern, variables, match.where())
        }
    }

    private fun handleEveryPath(pattern: PatternPart, variables: MutableSet<LogicalVariable>, where: Option<Where>) {
        val element = pattern.element()

        var variable: Variable? = element.variable().getOrElse { null }

        var stream = graph.nodes.parallelStream()

        if (element is NodePattern) {
            stream = handleNodePattern(element, stream, variables, where)
        } else if (element is RelationshipChain) {
            stream = handleRelationshipChain(element, stream, variables, where)
        }
    }

    private fun handleRelationshipChain(chain: RelationshipChain, streamIn: Stream<Node>, variables: MutableSet<LogicalVariable>, where: Option<Where>): Stream<Node> {
        var stream = streamIn

        // first, handle it like a normal node
        stream = handleNodePattern(chain.element() as NodePattern, stream, variables, where)

        // TODO: support relationships based on 'any' label
        // relationship = we need the label for now
        val relationship = chain.relationship()

        if (relationship.direction() is SemanticDirection.`BOTH$`) {
            TODO("Not supporting relationships with both directions yet")
        }

        if (relationship.types().isEmpty) {
            TODO("Not supporting relationships without type yet")
        } else {
            val type = relationship.types().head()

            stream = stream.filter {
                var relationshipProperty = it[type.name().toLowerCase(), relationship.direction().toString()]

                // check for the existence of the edge
                if (relationshipProperty is Collection<*>) {
                    // TODO: check if it really needs unwrapping, not all our nodes are modelled this way
                    relationshipProperty = PropertyEdge.unwrap(relationshipProperty as MutableList<PropertyEdge<Node>>)

                    // TODO: save these sub streams somehow or use flatmap?
                    var subStream = relationshipProperty.stream() as Stream<Node>
                    if (chain.rightNode() is NodePattern) {
                        subStream = handleNodePattern(chain.rightNode(), subStream, variables, where)
                    } else {
                        TODO()
                    }

                    subStream.count() > 0
                } else {
                    relationshipProperty != null
                }
            }

            // hacky, this is already done by handleNodePattern but we need to update the stream
            val o = (chain.element() as NodePattern).variable()
            val variable: Variable? = o.getOrElse { null }

            // update stream
            variable?.let { streams[it] = stream }
        }

        return stream
    }

    private fun handleNodePattern(element: NodePattern, streamIn: Stream<Node>, variables: MutableSet<LogicalVariable>, where: Option<Where>): Stream<Node> {
        var stream = streamIn
        val labels = element.labels()

        // only one label for now
        if (!labels.isEmpty) {
            val label = labels.head()

            stream = stream.filter {
                it.labels.contains(label.name())
            }
        }

        // variable seems optional
        val o = element.variable()
        val variable: Variable? = o.getOrElse { null }

        // add to variables
        variable?.let { variables += it }

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

            // only select it, if contains a variable bound for this node pattern; or no variables are present
            if (list.isEmpty() || list.contains(variable)) {
                if (expression is Equals) {
                    stream = handleEquals(expression, stream)
                } else if (expression is LessThan) {
                    stream = handleLessThan(expression, stream)
                } else if (expression is GreaterThan) {
                    stream = handleGreaterThan(expression, stream)
                } else {
                    TODO()
                }
            }
        }

        variable?.let { streams[it] = stream }

        return stream
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

    private val parser = CypherParser()

    fun size(): Int {
        return nodes.size
    }

    operator fun plusAssign(node: Node) {
        nodes += node
    }

    fun query(queryText: String): List<Node> {
        val query = parser.parse(queryText, null) as Query

        return executeQuery(query)
    }

    @OptIn(ExperimentalTime::class)
    fun executeQuery(query: Query): List<Node> {
        var ctx = QueryContext(this)

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

        println("Query took ${b.duration.milliseconds}")

        return list
    }
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

// TODO: do this once and cache it
val Node.labels: List<String>
    get() {
        // for now, just the class itself
        // TODO: hierarchy

        val labels = mutableListOf<String>()

        labels += this.javaClass.simpleName

        return labels
    }

