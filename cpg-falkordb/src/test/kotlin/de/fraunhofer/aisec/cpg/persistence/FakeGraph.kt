/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.persistence

import com.falkordb.Driver
import com.falkordb.Graph
import com.falkordb.ResultSet
import redis.clients.jedis.Jedis
import redis.clients.jedis.exceptions.JedisDataException

/**
 * A [Graph] that records the queries it is asked to run instead of talking to a database. This lets
 * us assert on the Cypher that [FalkorDBDatabase] generates without needing a running FalkorDB
 * instance.
 *
 * @param failWith an optional exception that every [query] and [deleteGraph] call throws
 */
class FakeGraph(private val failWith: JedisDataException? = null) : Graph {

    /** All queries that were passed to [query], in order. */
    val queries = mutableListOf<String>()

    /** How often [deleteGraph] was called. */
    var deleteGraphCalls = 0
        private set

    /** How often [close] was called. */
    var closeCalls = 0
        private set

    override fun query(query: String): ResultSet? {
        failWith?.let { throw it }
        queries += query
        return null
    }

    override fun deleteGraph(): String {
        deleteGraphCalls++
        failWith?.let { throw it }
        return "OK"
    }

    override fun close() {
        closeCalls++
    }

    override fun readOnlyQuery(query: String): ResultSet? = query(query)

    override fun query(query: String, timeout: Long): ResultSet? = query(query)

    override fun readOnlyQuery(query: String, timeout: Long): ResultSet? = query(query)

    override fun query(query: String, params: MutableMap<String, Any>?): ResultSet? = query(query)

    override fun readOnlyQuery(query: String, params: MutableMap<String, Any>?): ResultSet? =
        query(query)

    override fun query(query: String, params: MutableMap<String, Any>?, timeout: Long): ResultSet? =
        query(query)

    override fun readOnlyQuery(
        query: String,
        params: MutableMap<String, Any>?,
        timeout: Long,
    ): ResultSet? = query(query)

    override fun callProcedure(procedure: String): ResultSet? = query(procedure)

    override fun callProcedure(procedure: String, args: MutableList<String>?): ResultSet? =
        query(procedure)

    override fun callProcedure(
        procedure: String,
        args: MutableList<String>?,
        kwargs: MutableMap<String, MutableList<String>>?,
    ): ResultSet? = query(procedure)

    override fun copyGraph(destination: String): String = destination
}

/** A [Driver] that only records whether it was closed. */
class FakeDriver : Driver {

    /** How often [close] was called. */
    var closeCalls = 0
        private set

    override fun close() {
        closeCalls++
    }

    override fun graph(graphId: String) = throw UnsupportedOperationException()

    override fun getConnection(): Jedis = throw UnsupportedOperationException()
}
