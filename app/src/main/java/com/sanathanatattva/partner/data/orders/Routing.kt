package com.sanathanatattva.partner.data.orders

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** A road route from OSRM (handoff §10): the line drawn on in-app maps. Navigation itself is Google Maps. */
data class Route(val points: List<Pair<Double, Double>>, val metres: Double, val seconds: Double)

object Routing {
    /**
     * The public OSRM demo server is fine for testing but not allowed for production traffic:
     * self-host OSRM with the Karnataka extract and point this at it before launch.
     */
    private const val OSRM = "https://router.project-osrm.org/route/v1/driving"

    private val client = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = ConcurrentHashMap<String, Route>()

    /** lat/lng pairs; rounded to ~10 m so a jittery GPS doesn't refetch every few seconds. */
    suspend fun route(from: Pair<Double, Double>, to: Pair<Double, Double>): Route? {
        fun r(v: Double) = "%.4f".format(java.util.Locale.ROOT, v)
        val key = "${r(from.second)},${r(from.first)};${r(to.second)},${r(to.first)}"
        cache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url("$OSRM/$key?overview=full&geometries=geojson").build()).execute().use { res ->
                    if (!res.isSuccessful) return@use null
                    val body = json.parseToJsonElement(res.body?.string() ?: return@use null).jsonObject
                    val first = body["routes"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@use null
                    val coords = first["geometry"]!!.jsonObject["coordinates"]!!.jsonArray.map { c ->
                        val p = c.jsonArray
                        p[1].jsonPrimitive.double to p[0].jsonPrimitive.double
                    }
                    Route(coords, first["distance"]!!.jsonPrimitive.double, first["duration"]!!.jsonPrimitive.double)
                }
            }.getOrNull()?.also { cache[key] = it }
        }
    }
}
